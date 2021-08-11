package com.rarible.protocol.order.core.converters.model

import com.rarible.protocol.dto.*
import com.rarible.protocol.order.core.model.OrderType
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
object OrderTypeConverter : Converter<OrderFormDto, OrderType> {
    override fun convert(source: OrderFormDto): OrderType {
        return when (source) {
            is LegacyOrderFormDto -> OrderType.RARIBLE_V1
            is RaribleV2OrderFormDto -> OrderType.RARIBLE_V2
            is OpenSeaV1OrderFormDto -> OrderType.OPEN_SEA_V1
        }
    }
}

