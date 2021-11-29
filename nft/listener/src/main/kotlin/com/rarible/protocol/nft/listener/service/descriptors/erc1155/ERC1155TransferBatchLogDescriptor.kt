package com.rarible.protocol.nft.listener.service.descriptors.erc1155

import com.rarible.contracts.erc1155.TransferBatchEvent
import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.ItemTransfer
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.listener.service.descriptors.ItemHistoryLogEventDescriptor
import com.rarible.protocol.nft.core.service.token.TokenRegistrationService
import io.daonomic.rpc.domain.Word
import org.reactivestreams.Publisher
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import scalether.domain.Address
import scalether.domain.response.Log
import java.time.Instant

@Service
@CaptureSpan(type = SpanType.EVENT)
class ERC1155TransferBatchLogDescriptor(
    private val tokenRegistrationService: TokenRegistrationService
) : ItemHistoryLogEventDescriptor<ItemTransfer> {

    override fun convert(log: Log, date: Instant): Publisher<ItemTransfer> {
        return tokenRegistrationService.getTokenStandard(log.address())
            .flatMapMany { standard ->
                if (standard == TokenStandard.ERC1155) {
                    val e = TransferBatchEvent.apply(log)
                    if (e._ids().size != e._values().size) {
                        Mono.error<ItemTransfer>(IllegalStateException("ids.size != values.size"))
                    } else if (e._from() == Address.ZERO() && e._to() == Address.ZERO()) {
                        Mono.empty<ItemTransfer>()
                    } else {
                        e._ids().zip(e._values())
                            .map {
                                ItemTransfer(
                                    from = e._from(),
                                    owner = e._to(),
                                    token = log.address(),
                                    tokenId = EthUInt256.of(it.first),
                                    date = date,
                                    value = EthUInt256.of(it.second)
                                )
                            }
                            .toFlux()
                    }
                } else {
                    Mono.empty<ItemTransfer>()
                }
            }
    }

    override val topic: Word = TransferBatchEvent.id()

    override fun getAddresses(): Mono<Collection<Address>> {
        return Mono.just(emptyList())
    }
}
