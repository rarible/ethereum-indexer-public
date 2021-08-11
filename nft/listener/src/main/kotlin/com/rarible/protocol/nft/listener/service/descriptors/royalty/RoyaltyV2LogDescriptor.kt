package com.rarible.protocol.nft.listener.service.descriptors.royalty

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.royalties.RoyaltiesSetEvent
import com.rarible.protocol.nft.core.model.ItemRoyalty
import com.rarible.protocol.nft.core.model.Part
import com.rarible.protocol.nft.listener.service.descriptors.ItemHistoryLogEventDescriptor
import io.daonomic.rpc.domain.Word
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import scalether.domain.Address
import scalether.domain.response.Log
import java.time.Instant

@Service
class RoyaltyV2LogDescriptor: ItemHistoryLogEventDescriptor<ItemRoyalty> {
    override val topic: Word = RoyaltiesSetEvent.id()

    override fun convert(log: Log, date: Instant): Mono<ItemRoyalty> {
        val event = RoyaltiesSetEvent.apply(log)
        val royalties = mutableListOf<Part>()

        for (royalty in event.royalties()) {
            royalties.add(
                Part(
                    royalty._1(),
                    royalty._2().toInt()
                )
            )
        }
        return ItemRoyalty(
            token = log.address(),
            tokenId = EthUInt256.of(event.tokenId()),
            date = date,
            royalties = royalties
        ).toMono()
    }

    override fun getAddresses(): Mono<Collection<Address>> {
        return Mono.just(emptyList())
    }
}
