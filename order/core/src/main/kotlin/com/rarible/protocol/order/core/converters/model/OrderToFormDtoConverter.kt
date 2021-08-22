package com.rarible.protocol.order.core.converters.model

import com.rarible.protocol.dto.*
import com.rarible.protocol.order.core.converters.dto.AssetDtoConverter
import com.rarible.protocol.order.core.converters.dto.OrderDataDtoConverter
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderType
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
object OrderToFormDtoConverter : Converter<Order, OrderFormDto> {
    override fun convert(source: Order): OrderFormDto {
        return when (source.type) {
            OrderType.RARIBLE_V1 -> LegacyOrderFormDto(
                maker = source.maker,
                make = AssetDtoConverter.convert(source.make),
                taker = source.taker,
                take = AssetDtoConverter.convert(source.take),
                salt = source.salt.value,
                signature = source.signature,
                start = source.start,
                end = source.end,
                data = OrderDataDtoConverter.convert(source.data) as OrderDataLegacyDto
            )
            OrderType.RARIBLE_V2 -> RaribleV2OrderFormDto(
                maker = source.maker,
                make = AssetDtoConverter.convert(source.make),
                taker = source.taker,
                take = AssetDtoConverter.convert(source.take),
                salt = source.salt.value,
                signature = source.signature,
                start = source.start,
                end = source.end,
                data = OrderDataDtoConverter.convert(source.data) as OrderRaribleV2DataV1Dto
            )
            OrderType.OPEN_SEA_V1 -> OpenSeaV1OrderFormDto(
                maker = source.maker,
                make = AssetDtoConverter.convert(source.make),
                taker = source.taker,
                take = AssetDtoConverter.convert(source.take),
                salt = source.salt.value,
                signature = source.signature,
                start = source.start,
                end = source.end,
                data = OrderDataDtoConverter.convert(source.data) as OrderOpenSeaV1DataV1Dto
            )
            OrderType.CRYPTO_PUNKS -> throw IllegalArgumentException("CryptoPunks order are created on-chain")
        }
    }
}
