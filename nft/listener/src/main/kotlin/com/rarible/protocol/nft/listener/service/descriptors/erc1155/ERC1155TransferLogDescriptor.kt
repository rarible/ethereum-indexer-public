package com.rarible.protocol.nft.listener.service.descriptors.erc1155

import com.rarible.contracts.erc1155.TransferSingleEvent
import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.erc1155.TransferSingleEventTopics1
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
class ERC1155TransferLogDescriptor(
    private val tokenRegistrationService: TokenRegistrationService
) : ItemHistoryLogEventDescriptor<ItemTransfer> {

    override fun convert(log: Log, date: Instant): Mono<ItemTransfer> {
        return tokenRegistrationService.getTokenStandard(log.address())
            .flatMap { standard ->
                if (standard == TokenStandard.ERC1155) {
                    val e = when (log.topics().size()) {
                        1 -> TransferSingleEventTopics1.apply(log)
                        else -> TransferSingleEvent.apply(log)
                    }
                    if (e._from() == Address.ZERO() && e._to() == Address.ZERO()) {
                        Mono.empty()
                    } else {
                        ItemTransfer(
                            from = e._from(),
                            owner = e._to(),
                            token = log.address(),
                            tokenId = EthUInt256.of(e._id()),
                            date = date,
                            value = EthUInt256.of(e._value())
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
