package com.rarible.protocol.order.core.converters.model

import com.rarible.protocol.dto.LegacyOrderFormDto
import com.rarible.protocol.dto.OrderFormDto
import com.rarible.protocol.dto.RaribleV2OrderFormDto
import com.rarible.protocol.order.core.model.OrderType
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
object OrderTypeConverter : Converter<OrderFormDto, OrderType> {
    override fun convert(source: OrderFormDto): OrderType {
        return when (source) {
            is LegacyOrderFormDto -> OrderType.RARIBLE_V1
            is RaribleV2OrderFormDto -> OrderType.RARIBLE_V2
        }
    }
}

