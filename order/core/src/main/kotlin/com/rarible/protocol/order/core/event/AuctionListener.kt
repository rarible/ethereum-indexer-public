package com.rarible.protocol.order.core.event

import com.rarible.protocol.dto.AuctionDeleteEventDto
import com.rarible.protocol.dto.AuctionEventDto
import com.rarible.protocol.dto.AuctionUpdateEventDto
import com.rarible.protocol.order.core.converters.dto.AuctionDtoConverter
import com.rarible.protocol.order.core.model.Auction
import com.rarible.protocol.order.core.producer.ProtocolAuctionPublisher
import io.daonomic.rpc.domain.Word
import org.springframework.stereotype.Component
import java.util.*

@Component
class AuctionListener(
    private val auctionDtoConverter: AuctionDtoConverter,
    private val eventPublisher: ProtocolAuctionPublisher
) {
    suspend fun onAuctionUpdate(auction: Auction) {
        publish(
            AuctionUpdateEventDto(
                eventId = auction.lastEventId ?: UUID.randomUUID().toString(),
                auctionId = auction.hash.toString(),
                auction = auctionDtoConverter.convert(auction)
            )
        )
    }

    suspend fun onAuctionDelete(hash: Word) {
        publish(
            AuctionDeleteEventDto(
                eventId = hash.hex(),
                auctionId = hash.hex()
            )
        )
    }

    private suspend fun publish(event: AuctionEventDto) = eventPublisher.publish(event)
}
