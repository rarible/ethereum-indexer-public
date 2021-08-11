package com.rarible.protocol.order.core.converters.dto

import com.rarible.protocol.dto.OrderCancelDto
import com.rarible.protocol.dto.OrderExchangeHistoryDto
import com.rarible.protocol.dto.OrderSideDto
import com.rarible.protocol.dto.OrderSideMatchDto
import com.rarible.protocol.order.core.model.OrderCancel
import com.rarible.protocol.order.core.model.OrderExchangeHistory
import com.rarible.protocol.order.core.model.OrderSide
import com.rarible.protocol.order.core.model.OrderSideMatch
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
object OrderExchangeHistoryDtoConverter : Converter<OrderExchangeHistory, OrderExchangeHistoryDto> {
    override fun convert(source: OrderExchangeHistory): OrderExchangeHistoryDto {
        return when (source) {
            is OrderSideMatch -> OrderSideMatchDto(
                hash = source.hash,
                counterHash = source.counterHash,
                fill = source.fill.value,
                side = source.side?.let { convert(it) },
                make = source.make.let { AssetDtoConverter.convert(it) },
                take = source.take.let { AssetDtoConverter.convert(it) },
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
                make = source.make?.let { AssetDtoConverter.convert(it) },
                take = source.take?.let { AssetDtoConverter.convert(it) },
                date = source.date,
                maker = source.maker,
                owner = source.maker
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
