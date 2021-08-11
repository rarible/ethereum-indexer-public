package com.rarible.protocol.order.core.converters.dto

import com.rarible.protocol.dto.OrderDataLegacyDto
import com.rarible.protocol.order.core.model.OrderDataLegacy
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
object RaribleLegacyDataDtoConverter : Converter<OrderDataLegacy, OrderDataLegacyDto> {
    override fun convert(source: OrderDataLegacy): OrderDataLegacyDto {
        return OrderDataLegacyDto(source.fee)
    }
}
