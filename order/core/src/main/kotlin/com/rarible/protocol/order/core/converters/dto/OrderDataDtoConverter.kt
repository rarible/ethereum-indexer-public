package com.rarible.protocol.order.core.converters.dto

import com.rarible.protocol.dto.OrderDataDto
import com.rarible.protocol.order.core.model.OrderCryptoPunksData
import com.rarible.protocol.order.core.model.OrderData
import com.rarible.protocol.order.core.model.OrderDataLegacy
import com.rarible.protocol.order.core.model.OrderOpenSeaV1DataV1
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV1
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV2
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
object OrderDataDtoConverter: Converter<OrderData, OrderDataDto> {
    override fun convert(source: OrderData): OrderDataDto {
        return when(source) {
            is OrderRaribleV2DataV1 -> RaribleV2DataV1DtoConverter.convert(source)
            is OrderRaribleV2DataV2 -> RaribleV2DataV2DtoConverter.convert(source)
            is OrderDataLegacy -> RaribleLegacyDataDtoConverter.convert(source)
            is OrderOpenSeaV1DataV1 -> OpenSeaV1DataV1DtoConverter.convert(source)
            is OrderCryptoPunksData -> OrderCryptoPunksDataDtoConverter.convert(source)
        }
    }
}

