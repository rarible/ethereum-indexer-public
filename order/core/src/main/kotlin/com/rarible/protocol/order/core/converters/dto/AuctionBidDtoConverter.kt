package com.rarible.protocol.order.core.converters.dto

import com.rarible.protocol.dto.AuctionBidDto
import com.rarible.protocol.dto.RaribleAuctionV1BidDataV1Dto
import com.rarible.protocol.dto.RaribleAuctionV1BidV1Dto
import com.rarible.protocol.order.core.model.AssetType
import com.rarible.protocol.order.core.model.Bid
import com.rarible.protocol.order.core.model.BidV1
import com.rarible.protocol.order.core.service.PriceNormalizer
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class AuctionBidDtoConverter(
    val priceNormalizer: PriceNormalizer
) {
    suspend fun convert(assetType: AssetType, buyer: Address, source: Bid): AuctionBidDto {
        return when (source) {
            is BidV1 -> RaribleAuctionV1BidV1Dto(
                buyer = buyer,
                amount = priceNormalizer.normalize(assetType, source.amount.value),
                data = AuctionBidDataDtoConverter.convert(source.data) as RaribleAuctionV1BidDataV1Dto
            )
        }
    }
}
