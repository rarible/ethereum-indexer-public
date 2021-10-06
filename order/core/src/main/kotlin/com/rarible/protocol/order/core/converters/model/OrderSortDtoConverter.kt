package com.rarible.protocol.order.core.converters.model

import com.rarible.protocol.dto.OrderFilterDto
import com.rarible.protocol.dto.OrderSortDto
import org.springframework.core.convert.converter.Converter

object OrderSortDtoConverter : Converter<OrderSortDto, OrderFilterDto.Sort> {
    override fun convert(source: OrderSortDto): OrderFilterDto.Sort {
        return when (source) {
            OrderSortDto.LAST_UPDATE_DESC -> OrderFilterDto.Sort.LAST_UPDATE_DESC
            OrderSortDto.LAST_UPDATE_ASC -> OrderFilterDto.Sort.LAST_UPDATE_ASC
        }
    }
}
