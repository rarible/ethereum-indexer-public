package com.rarible.protocol.order.core.event

import com.rarible.protocol.dto.AuctionDeleteEventDto
import com.rarible.protocol.dto.AuctionEventDto
import com.rarible.protocol.dto.AuctionUpdateEventDto
import com.rarible.protocol.order.core.converters.dto.AuctionDtoConverter
import com.rarible.protocol.order.core.model.Auction
import com.rarible.protocol.order.core.producer.ProtocolAuctionPublisher
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
                auctionId = auction.hash.prefixed(),
                auction = auctionDtoConverter.convert(auction)
            )
        )
    }

    suspend fun onAuctionDelete(auction: Auction) {
        publish(
            AuctionDeleteEventDto(
                eventId = auction.hash.prefixed(),
                auctionId = auction.hash.prefixed(),
                auction = auctionDtoConverter.convert(auction)
            )
        )
    }

    private suspend fun publish(event: AuctionEventDto) = eventPublisher.publish(event)
}
