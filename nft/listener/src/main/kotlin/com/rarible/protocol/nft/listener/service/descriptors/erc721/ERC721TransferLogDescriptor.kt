package com.rarible.protocol.nft.listener.service.descriptors.erc721

import com.rarible.contracts.erc721.TransferEvent
import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.TransferEventWithFullData
import com.rarible.protocol.contracts.TransferEventWithNotFullData
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.converters.model.ItemIdFromStringConverter
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemTransfer
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.service.token.TokenRegistrationService
import com.rarible.protocol.nft.listener.service.descriptors.ItemHistoryLogEventDescriptor
import com.rarible.protocol.nft.listener.service.resolver.IgnoredTokenResolver
import com.rarible.protocol.nft.listener.service.item.CustomMintDetector
import io.daonomic.rpc.domain.Word
import org.reactivestreams.Publisher
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import scalether.domain.Address
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

@Service
@CaptureSpan(type = SpanType.EVENT)
class ERC721TransferLogDescriptor(
    private val tokenRegistrationService: TokenRegistrationService,
    private val customMintDetector: CustomMintDetector,
    ignoredTokenResolver: IgnoredTokenResolver,
    indexerProperties: NftIndexerProperties,
) : ItemHistoryLogEventDescriptor<ItemTransfer> {

    private val skipContracts = ignoredTokenResolver.resolve()
    private val skipTransferContractTokens = indexerProperties.scannerProperties.skipTransferContractTokens.map(
        ItemIdFromStringConverter::convert
    )
    private val ignoredStandards = listOf(TokenStandard.NONE, TokenStandard.CRYPTO_PUNKS)

    override val topic: Word = TransferEvent.id()

    override fun convert(
        log: Log,
        transaction: Transaction,
        timestamp: Long,
        index: Int,
        totalLogs: Int
    ): Publisher<ItemTransfer> {
        if (log.address() in skipContracts) {
            return Mono.empty()
        }
        val date = Instant.ofEpochSecond(timestamp)
        return tokenRegistrationService.getTokenStandard(log.address())
            .flatMap { standard ->
                if (standard !in ignoredStandards) {
                    val e = when (log.topics().size()) {
                        4 -> TransferEvent.apply(log)
                        1 -> TransferEventWithFullData.apply(log)
                        else -> TransferEventWithNotFullData.apply(log)
                    }
                    if (e.from() == Address.ZERO() && e.to() == Address.ZERO()) {
                        Mono.empty()
                    } else if (ItemId(log.address(), EthUInt256.of(e.tokenId())) in skipTransferContractTokens) {
                        Mono.empty()
                    } else {
                        val isMint = customMintDetector.isErc721Mint(e, transaction).takeIf { it }
                        ItemTransfer(
                            from = e.from(),
                            owner = e.to(),
                            token = log.address(),
                            tokenId = EthUInt256.of(e.tokenId()),
                            date = date,
                            value = EthUInt256.of(1),
                            isMint = isMint,
                            mintPrice = mintPrice(isMint, transaction.value(), totalLogs)
                        ).toMono()
                    }
                } else {
                    Mono.empty()
                }
            }
    }

    override fun convert(log: Log, transaction: Transaction, date: Instant): Publisher<ItemTransfer> {
        throw RuntimeException("Shouldn't call")
    }

    override fun getAddresses(): Mono<Collection<Address>> {
        return Mono.just(emptyList())
    }
}
