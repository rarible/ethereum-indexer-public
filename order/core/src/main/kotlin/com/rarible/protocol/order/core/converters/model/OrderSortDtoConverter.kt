package com.rarible.protocol.order.core.converters.model

import com.rarible.protocol.dto.OrderSortDto
import com.rarible.protocol.order.core.model.order.Filter
import org.springframework.core.convert.converter.Converter

object OrderSortDtoConverter : Converter<OrderSortDto, Filter.Sort> {
    override fun convert(source: OrderSortDto): Filter.Sort {
        return when (source) {
            OrderSortDto.LAST_UPDATE_DESC -> Filter.Sort.LAST_UPDATE_DESC
            OrderSortDto.LAST_UPDATE_ASC -> Filter.Sort.LAST_UPDATE_ASC
        }
    }
}
