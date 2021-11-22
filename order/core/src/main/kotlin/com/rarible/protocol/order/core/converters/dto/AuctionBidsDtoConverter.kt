package com.rarible.protocol.order.core.converters.dto

import com.rarible.protocol.order.core.model.AuctionBidEntityDto
import com.rarible.protocol.order.core.model.AuctionBidEntity
import org.springframework.stereotype.Component

@Component
class AuctionBidsDtoConverter(
    private val auctionBidDtoConverter: AuctionBidDtoConverter
) {
    suspend fun convert(source: AuctionBidEntity): AuctionBidEntityDto {
        return AuctionBidEntityDto(
            id = source.id,
            dto = auctionBidDtoConverter.convert(source.buy, source.buyer, source.bid)
        )
    }
}
