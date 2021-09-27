package com.rarible.protocol.order.core.converters.dto

import com.rarible.protocol.dto.*
import com.rarible.protocol.order.core.model.*
import org.springframework.stereotype.Component

@Component
class OrderExchangeHistoryDtoConverter(
    private val assetDtoConverter: AssetDtoConverter
) {

    suspend fun convert(source: OrderExchangeHistory): OrderExchangeHistoryDto {
        return when (source) {
            is OrderSideMatch -> OrderSideMatchDto(
                hash = source.hash,
                counterHash = source.counterHash,
                fill = source.fill.value,
                side = source.side?.let { convert(it) },
                make = source.make.let { assetDtoConverter.convert(it) },
                take = source.take.let { assetDtoConverter.convert(it) },
                date = source.date,
                maker = source.maker,
                taker = source.taker,
                makeUsd = source.makeUsd,
                takeUsd = source.takeUsd,
                makePriceUsd = source.makePriceUsd,
                takePriceUsd = source.takePriceUsd
            )
            is OrderCancel -> OrderCancelDto(
                hash = source.hash,
                make = source.make?.let { assetDtoConverter.convert(it) },
                take = source.take?.let { assetDtoConverter.convert(it) },
                date = source.date,
                maker = source.maker,
                owner = source.maker
            )
            is OnChainOrder -> OnChainOrderDto(
                hash = source.hash,
                make = assetDtoConverter.convert(source.make),
                take = assetDtoConverter.convert(source.take),
                date = source.date,
                maker = source.maker
            )
        }
    }

    private fun convert(source: OrderSide): OrderSideDto {
        return when (source) {
            OrderSide.LEFT -> OrderSideDto.LEFT
            OrderSide.RIGHT -> OrderSideDto.RIGHT
        }
    }
}
