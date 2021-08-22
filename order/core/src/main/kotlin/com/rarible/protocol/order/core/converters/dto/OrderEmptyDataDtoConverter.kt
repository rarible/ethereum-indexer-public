package com.rarible.protocol.order.core.converters.dto

import com.rarible.protocol.dto.OrderEmptyDataDto
import com.rarible.protocol.order.core.model.OrderEmptyData
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
object OrderEmptyDataDtoConverter : Converter<OrderEmptyData, OrderEmptyDataDto> {
    override fun convert(source: OrderEmptyData) = OrderEmptyDataDto()
}
