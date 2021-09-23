package com.rarible.protocol.nftorder.core.converter

import com.rarible.protocol.dto.*
import com.rarible.protocol.nftorder.core.model.ShortOrder

object ShortOrderConverter {

    fun convert(order: OrderDto): ShortOrder {

        return ShortOrder(
            hash = order.hash,
            platform = evalPlatform(order).name,
            makeStock = order.makeStock,
            fill = order.fill,
            takeValue = order.take.value,
            makePriceUsd = order.makePriceUsd,
            takePriceUsd = order.takePriceUsd,
            cancelled = order.cancelled
        )
    }

    private fun evalPlatform(order: OrderDto): PlatformDto {
        return when (order) {
            is LegacyOrderDto -> PlatformDto.RARIBLE
            is RaribleV2OrderDto -> PlatformDto.RARIBLE
            is OpenSeaV1OrderDto -> PlatformDto.OPEN_SEA
            is CryptoPunkOrderDto -> PlatformDto.CRYPTO_PUNKS
        }

    }
}