package com.rarible.protocol.order.core.converters.model

import com.rarible.protocol.dto.AuctionSortDto
import com.rarible.protocol.order.core.repository.auction.AuctionFilter

object AuctionSortConverter {
    fun convert(source: AuctionSortDto): AuctionFilter.AuctionSort {
        return when (source) {
            AuctionSortDto.LAST_UPDATE_ASC -> AuctionFilter.AuctionSort.LAST_UPDATE_ASC
            AuctionSortDto.LAST_UPDATE_DESC -> AuctionFilter.AuctionSort.LAST_UPDATE_DESC
            AuctionSortDto.BUY_PRICE_ASC -> AuctionFilter.AuctionSort.BUY_PRICE_ASC
        }
    }
}

