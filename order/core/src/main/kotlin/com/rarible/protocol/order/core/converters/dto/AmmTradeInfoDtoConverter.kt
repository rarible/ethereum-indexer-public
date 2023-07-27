package com.rarible.protocol.order.core.converters.dto

import com.rarible.protocol.dto.AmmPriceInfoDto
import com.rarible.protocol.dto.AmmTradeInfoDto
import com.rarible.protocol.order.core.model.PoolTradePrice

object AmmTradeInfoDtoConverter {
    fun convert(source: List<PoolTradePrice>): AmmTradeInfoDto {
        return AmmTradeInfoDto(
            prices = source.map { price ->
                AmmPriceInfoDto(
                    price = price.price,
                    priceValue = price.priceValue,
                    priceUsd = price.priceUsd
                )
            }
        )
    }
}
