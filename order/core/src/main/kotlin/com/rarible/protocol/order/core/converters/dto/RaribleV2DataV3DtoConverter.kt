package com.rarible.protocol.order.core.converters.dto

import com.rarible.protocol.dto.OrderRaribleV2DataV3BuyDto
import com.rarible.protocol.dto.OrderRaribleV2DataV3SellDto
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV3Buy
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV3Sell

object RaribleV2DataV3DtoConverter {
    fun convert(source: OrderRaribleV2DataV3Sell): OrderRaribleV2DataV3SellDto {
        return OrderRaribleV2DataV3SellDto(
            payout = source.payout?.let { PartListDtoConverter.convert(it) },
            originFeeFirst = source.originFeeFirst?.let { PartListDtoConverter.convert(it) },
            originFeeSecond = source.originFeeSecond?.let { PartListDtoConverter.convert(it) },
            maxFeesBasePoint = source.maxFeesBasePoint.value.intValueExact(),
            marketplaceMarker = source.marketplaceMarker
        )
    }

    fun convert(source: OrderRaribleV2DataV3Buy): OrderRaribleV2DataV3BuyDto {
        return OrderRaribleV2DataV3BuyDto(
            payout = source.payout?.let { PartListDtoConverter.convert(it) },
            originFeeFirst = source.originFeeFirst?.let { PartListDtoConverter.convert(it) },
            originFeeSecond = source.originFeeSecond?.let { PartListDtoConverter.convert(it) },
            marketplaceMarker = source.marketplaceMarker
        )
    }
}
