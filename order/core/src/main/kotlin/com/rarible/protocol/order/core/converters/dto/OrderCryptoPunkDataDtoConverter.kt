package com.rarible.protocol.order.core.converters.dto

import com.rarible.protocol.dto.OrderCryptoPunksDataDto
import com.rarible.protocol.order.core.model.OrderCryptoPunksData
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
object OrderCryptoPunksDataDtoConverter : Converter<OrderCryptoPunksData, OrderCryptoPunksDataDto> {
    override fun convert(source: OrderCryptoPunksData) = OrderCryptoPunksDataDto()
}
