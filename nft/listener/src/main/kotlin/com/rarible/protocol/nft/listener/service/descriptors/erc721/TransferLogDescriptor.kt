package com.rarible.protocol.nft.listener.service.descriptors.erc721

import com.rarible.contracts.erc721.TransferEvent
import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.TransferEventWithFullData
import com.rarible.protocol.contracts.TransferEventWithNotFullData
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemTransfer
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.service.token.TokenRegistrationService
import com.rarible.protocol.nft.listener.service.descriptors.ItemHistoryLogEventDescriptor
import io.daonomic.rpc.domain.Word
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import scalether.domain.Address
import scalether.domain.response.Log
import java.time.Instant

@Service
@CaptureSpan(type = SpanType.EVENT)
class TransferLogDescriptor(
    private val tokenRegistrationService: TokenRegistrationService,
    properties: NftIndexerProperties
) : ItemHistoryLogEventDescriptor<ItemTransfer> {

    private val skipTransferContractTokens = properties.scannerProperties.skipTransferContractTokens
    private val ignoredStandards = listOf(TokenStandard.NONE, TokenStandard.CRYPTO_PUNKS)

    override val topic: Word = TransferEvent.id()

    override fun convert(log: Log, date: Instant): Mono<ItemTransfer> {
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
                        ItemTransfer(
                            from = e.from(),
                            owner = e.to(),
                            token = log.address(),
                            tokenId = EthUInt256.of(e.tokenId()),
                            date = date,
                            value = EthUInt256.of(1)
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
