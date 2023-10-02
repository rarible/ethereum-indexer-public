package com.rarible.protocol.nft.listener.service.descriptors.erc721

import com.rarible.contracts.erc721.TransferEvent
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.TransferEventWithFullData
import com.rarible.protocol.contracts.TransferEventWithNotFullData
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
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import scalether.domain.Address
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

@Service
class ERC721TransferLogDescriptor(
    private val tokenService: TokenService,
    private val customMintDetector: CustomMintDetector,
    ignoredTokenResolver: IgnoredTokenResolver,
    indexerProperties: NftIndexerProperties,
) : ItemHistoryLogEventDescriptor<ItemTransfer> {

    private val skipContracts = ignoredTokenResolver.resolve()
    private val skipTransferContractTokens = indexerProperties.scannerProperties.skipTransferContractTokens.map(
        ItemIdFromStringConverter::convert
    )
    private val ignoredStandards = listOf(TokenStandard.NONE, TokenStandard.CRYPTO_PUNKS, TokenStandard.ERC20)

    override val topic: Word = TransferEvent.id()

    override fun convert(log: Log, transaction: Transaction, date: Instant): Mono<ItemTransfer> {
        if (log.address() in skipContracts) {
            return Mono.empty()
        }
        return mono { tokenService.getTokenStandard(log.address()) }
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
                        ItemTransfer(
                            from = e.from(),
                            owner = e.to(),
                            token = log.address(),
                            tokenId = EthUInt256.of(e.tokenId()),
                            date = date,
                            value = EthUInt256.of(1),
                            isMint = customMintDetector.isErc721Mint(e, transaction).takeIf { it }
                        ).toMono()
                    }
                } else {
                    Mono.empty()
                }
            }
    }

    override fun getAddresses(): Mono<Collection<Address>> {
        return Mono.just(emptyList())
    }
}
