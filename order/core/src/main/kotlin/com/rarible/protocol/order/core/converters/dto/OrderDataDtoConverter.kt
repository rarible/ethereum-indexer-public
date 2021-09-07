package com.rarible.protocol.order.core.converters.dto

import com.rarible.protocol.dto.OrderDataDto
import com.rarible.protocol.order.core.model.*
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
object OrderDataDtoConverter: Converter<OrderData, OrderDataDto> {
    override fun convert(source: OrderData): OrderDataDto {
        return when(source) {
            is OrderRaribleV2DataV1 -> RaribleV2DataV1DtoConverter.convert(source)
            is OrderDataLegacy -> RaribleLegacyDataDtoConverter.convert(source)
            is OrderOpenSeaV1DataV1 -> OpenSeaV1DataV1DtoConverter.convert(source)
            is OrderCryptoPunksData -> OrderCryptoPunksDataDtoConverter.convert(source)
        }
    }
}

