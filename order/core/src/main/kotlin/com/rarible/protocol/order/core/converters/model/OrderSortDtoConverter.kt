package com.rarible.protocol.order.core.converters.model

import com.rarible.protocol.dto.OrderSortDto
import com.rarible.protocol.order.core.model.order.OrderFilter
import com.rarible.protocol.order.core.model.order.OrderFilterSort
import org.springframework.core.convert.converter.Converter

object OrderSortDtoConverter : Converter<OrderSortDto, OrderFilterSort> {
    override fun convert(source: OrderSortDto): OrderFilterSort {
        return when (source) {
            OrderSortDto.LAST_UPDATE_DESC -> OrderFilterSort.LAST_UPDATE_DESC
            OrderSortDto.LAST_UPDATE_ASC -> OrderFilterSort.LAST_UPDATE_ASC
        }
    }
}
