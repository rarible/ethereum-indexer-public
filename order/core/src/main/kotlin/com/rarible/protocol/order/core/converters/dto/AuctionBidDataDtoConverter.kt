package com.rarible.protocol.order.core.converters.dto

import com.rarible.protocol.dto.AuctionBidDataDto
import com.rarible.protocol.dto.RaribleAuctionV1BidDataV1Dto
import com.rarible.protocol.order.core.model.BidData
import com.rarible.protocol.order.core.model.BidDataV1

object AuctionBidDataDtoConverter {
    fun convert(source: BidData): AuctionBidDataDto {
        return when (source) {
            is BidDataV1 -> RaribleAuctionV1BidDataV1Dto(
                originFees = PartListDtoConverter.convert(source.originFees),
                payouts = PartListDtoConverter.convert(source.payouts)
            )
        }
    }
}
