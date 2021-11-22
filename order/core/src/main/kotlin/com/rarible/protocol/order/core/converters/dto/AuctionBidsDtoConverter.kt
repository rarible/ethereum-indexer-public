package com.rarible.protocol.order.core.converters.dto

import com.rarible.protocol.dto.AuctionBidDto
import com.rarible.protocol.order.core.model.AuctionBids
import org.springframework.stereotype.Component

@Component
class AuctionBidsDtoConverter(
    private val auctionBidDtoConverter: AuctionBidDtoConverter
) {
    suspend fun convert(source: AuctionBids): List<AuctionBidDto> {
        val (bidsPlaced, auction) = source
        return bidsPlaced.map { bidPlaced ->
            auctionBidDtoConverter.convert(auction.buy, bidPlaced.buyer, bidPlaced.bid)
        }
    }
}
