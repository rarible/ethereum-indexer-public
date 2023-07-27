package com.rarible.protocol.order.core.converters.dto

import com.rarible.protocol.dto.AuctionDataDto
import com.rarible.protocol.dto.AuctionDto
import com.rarible.protocol.dto.PartDto
import com.rarible.protocol.dto.RaribleAuctionV1BidV1Dto
import com.rarible.protocol.dto.RaribleAuctionV1DataV1Dto
import com.rarible.protocol.dto.RaribleAuctionV1Dto
import com.rarible.protocol.order.core.model.AssetType
import com.rarible.protocol.order.core.model.Auction
import com.rarible.protocol.order.core.model.AuctionBidEntity
import com.rarible.protocol.order.core.model.AuctionData
import com.rarible.protocol.order.core.model.AuctionType
import com.rarible.protocol.order.core.model.Part
import com.rarible.protocol.order.core.model.RaribleAuctionV1DataV1
import com.rarible.protocol.order.core.service.PriceNormalizer
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.BigInteger

@Component
class AuctionDtoConverter(
    private val priceNormalizer: PriceNormalizer,
    private val assetDtoConverter: AssetDtoConverter,
    private val auctionBidDtoConverter: AuctionBidDtoConverter
) {
    suspend fun convert(source: Auction): AuctionDto {
        return when (source.type) {
            AuctionType.RARIBLE_V1 -> RaribleAuctionV1Dto(
                contract = source.contract,
                seller = source.seller,
                sell = assetDtoConverter.convert(source.sell),
                buy = AssetTypeDtoConverter.convert(source.buy),
                endTime = source.endTime,
                minimalStep = normalizerPrice(source.buy, source.minimalStep.value),
                minimalPrice = normalizerPrice(source.buy, source.minimalPrice.value),
                createdAt = source.createdAt,
                lastUpdateAt = source.lastUpdateAt,
                buyPrice = source.buyPrice,
                buyPriceUsd = source.buyPriceUsd,
                pending = emptyList(),
                status = AuctionStatusDtoConverter.convert(source.status),
                ongoing = source.ongoing,
                hash = source.hash,
                auctionId = source.auctionId.value,
                lastBid = source.lastBid?.let { lastBid ->
                    source.buyer?.let { buyer ->
                        auctionBidDtoConverter.convert(source.buy, buyer, lastBid, lastBid.date, AuctionBidEntity.Status.ACTIVE) as RaribleAuctionV1BidV1Dto
                    }
                },
                data = convert(source.buy, source.data) as RaribleAuctionV1DataV1Dto
            )
        }
    }

    suspend fun convert(assetType: AssetType, source: AuctionData): AuctionDataDto {
        return when (source) {
            is RaribleAuctionV1DataV1 -> RaribleAuctionV1DataV1Dto(
                originFees = convert(source.originFees),
                payouts = convert(source.payouts),
                startTime = source.startTime,
                duration = source.duration.value,
                buyOutPrice = source.buyOutPrice?.value?.let { normalizerPrice(assetType, it) }
            )
        }
    }

    private fun convert(source: List<Part>): List<PartDto> {
        return source.map { PartDto(it.account, it.value.value.intValueExact()) }
    }

    private suspend fun normalizerPrice(assetType: AssetType, value: BigInteger): BigDecimal {
        return priceNormalizer.normalize(assetType, value)
    }
}
