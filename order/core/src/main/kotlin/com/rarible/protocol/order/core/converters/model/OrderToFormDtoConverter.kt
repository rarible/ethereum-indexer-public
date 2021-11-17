package com.rarible.protocol.order.core.converters.model

import com.rarible.protocol.dto.LegacyOrderFormDto
import com.rarible.protocol.dto.OrderDataLegacyDto
import com.rarible.protocol.dto.OrderFormDto
import com.rarible.protocol.dto.OrderRaribleV2DataDto
import com.rarible.protocol.dto.RaribleV2OrderFormDto
import com.rarible.protocol.order.core.converters.dto.OrderDataDtoConverter
import com.rarible.protocol.order.core.converters.dto.OrderFormAssetDtoConverter
import com.rarible.protocol.order.core.misc.orEmpty
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderType
import org.springframework.stereotype.Component

@Component
class OrderToFormDtoConverter {

    suspend fun convert(source: Order): OrderFormDto {
        return when (source.type) {
            OrderType.RARIBLE_V1 -> LegacyOrderFormDto(
                maker = source.maker,
                make = OrderFormAssetDtoConverter.convert(source.make),
                taker = source.taker,
                take = OrderFormAssetDtoConverter.convert(source.take),
                salt = source.salt.value,
                signature = source.signature.orEmpty(),
                start = source.start,
                end = source.end,
                data = OrderDataDtoConverter.convert(source.data) as OrderDataLegacyDto
            )
            OrderType.RARIBLE_V2 -> RaribleV2OrderFormDto(
                maker = source.maker,
                make = OrderFormAssetDtoConverter.convert(source.make),
                taker = source.taker,
                take = OrderFormAssetDtoConverter.convert(source.take),
                salt = source.salt.value,
                signature = source.signature.orEmpty(),
                start = source.start,
                end = source.end,
                data = OrderDataDtoConverter.convert(source.data) as OrderRaribleV2DataDto
            )
            OrderType.OPEN_SEA_V1 -> throw IllegalArgumentException("OpenSea order can't be created or updated")
            OrderType.CRYPTO_PUNKS -> throw IllegalArgumentException("CryptoPunks order are created on-chain")
        }
    }
}
