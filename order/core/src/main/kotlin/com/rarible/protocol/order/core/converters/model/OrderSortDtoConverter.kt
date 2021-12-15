package com.rarible.protocol.order.core.converters.model

import com.rarible.protocol.dto.OrderSortDto
import com.rarible.protocol.order.core.model.OrderFilter
import org.springframework.core.convert.converter.Converter

object OrderSortDtoConverter : Converter<OrderSortDto, OrderFilter.Sort> {
    override fun convert(source: OrderSortDto): OrderFilter.Sort {
        return when (source) {
            OrderSortDto.LAST_UPDATE_DESC -> OrderFilter.Sort.LAST_UPDATE_DESC
            OrderSortDto.LAST_UPDATE_ASC -> OrderFilter.Sort.LAST_UPDATE_ASC
        }
    }
}
