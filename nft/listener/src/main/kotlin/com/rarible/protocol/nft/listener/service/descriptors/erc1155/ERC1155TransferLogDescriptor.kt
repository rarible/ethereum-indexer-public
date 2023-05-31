package com.rarible.protocol.nft.listener.service.descriptors.erc1155

import com.rarible.contracts.erc1155.TransferSingleEvent
import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.erc1155.TransferSingleEventTopics1
import com.rarible.protocol.contracts.erc1155.TransferSingleEventTopics3
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.converters.model.ItemIdFromStringConverter
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemTransfer
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.service.token.TokenService
import com.rarible.protocol.nft.listener.service.descriptors.ItemHistoryLogEventDescriptor
import com.rarible.protocol.nft.listener.service.item.CustomMintDetector
import com.rarible.protocol.nft.listener.service.resolver.IgnoredTokenResolver
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import scalether.domain.Address
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

@Service
@CaptureSpan(type = SpanType.EVENT)
class ERC1155TransferLogDescriptor(
    private val tokenService: TokenService,
    private val customMintDetector: CustomMintDetector,
    ignoredTokenResolver: IgnoredTokenResolver,
    indexerProperties: NftIndexerProperties
) : ItemHistoryLogEventDescriptor<ItemTransfer> {

    private val skipContracts = ignoredTokenResolver.resolve()
    private val skipTransferContractTokens = indexerProperties.scannerProperties.skipTransferContractTokens.map(ItemIdFromStringConverter::convert)
    private val logger = LoggerFactory.getLogger(ERC1155TransferLogDescriptor::class.java)

    init {
        logger.info("Creating ERC1155TransferLogDescriptor, found ${skipContracts.size} skip tokens")
    }

    override fun convert(log: Log, transaction: Transaction, date: Instant): Mono<ItemTransfer> {
        if (log.address() in skipContracts) {
            return Mono.empty()
        }

        return mono { tokenService.getTokenStandard(log.address()) }
            .flatMap { standard ->
                if (standard == TokenStandard.ERC1155) {
                    val e = when (log.topics().size()) {
                        1 -> TransferSingleEventTopics1.apply(log)
                        3 -> TransferSingleEventTopics3.apply(log)
                        else -> TransferSingleEvent.apply(log)
                    }
                    if (e._from() == Address.ZERO() && e._to() == Address.ZERO()) {
                        Mono.empty()
                    } else if (ItemId(log.address(), EthUInt256.of(e._id())) in skipTransferContractTokens) {
                        Mono.empty()
                    } else {
                        ItemTransfer(
                            from = e._from(),
                            owner = e._to(),
                            token = log.address(),
                            tokenId = EthUInt256.of(e._id()),
                            date = date,
                            value = EthUInt256.of(e._value()),
                            isMint = customMintDetector.isErc1155Mint(e, transaction).takeIf { it }
                        ).toMono()
                    }
                } else {
                    Mono.empty()
                }
            }
    }

    override val topic: Word = TransferSingleEvent.id()

    override fun getAddresses(): Mono<Collection<Address>> {
        return Mono.just(emptyList())
    }
}
