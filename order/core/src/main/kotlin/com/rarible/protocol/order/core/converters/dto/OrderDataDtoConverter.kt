package com.rarible.protocol.order.core.converters.dto

import com.rarible.protocol.dto.OrderDataDto
import com.rarible.protocol.order.core.model.OrderBasicSeaportDataV1
import com.rarible.protocol.order.core.model.OrderCryptoPunksData
import com.rarible.protocol.order.core.model.OrderData
import com.rarible.protocol.order.core.model.OrderDataLegacy
import com.rarible.protocol.order.core.model.OrderLooksrareDataV1
import com.rarible.protocol.order.core.model.OrderLooksrareDataV2
import com.rarible.protocol.order.core.model.OrderOpenSeaV1DataV1
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV1
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV2
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV3Buy
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV3Sell
import com.rarible.protocol.order.core.model.OrderSudoSwapAmmDataV1
import com.rarible.protocol.order.core.model.OrderX2Y2DataV1
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
object OrderDataDtoConverter : Converter<OrderData, OrderDataDto> {
    override fun convert(source: OrderData): OrderDataDto {
        return when (source) {
            is OrderRaribleV2DataV1 -> RaribleV2DataV1DtoConverter.convert(source)
            is OrderRaribleV2DataV2 -> RaribleV2DataV2DtoConverter.convert(source)
            is OrderDataLegacy -> RaribleLegacyDataDtoConverter.convert(source)
            is OrderOpenSeaV1DataV1 -> OpenSeaV1DataV1DtoConverter.convert(source)
            is OrderBasicSeaportDataV1 -> SeaportDataV1DtoConverter.convert(source)
            is OrderCryptoPunksData -> OrderCryptoPunksDataDtoConverter.convert(source)
            is OrderRaribleV2DataV3Buy -> RaribleV2DataV3DtoConverter.convert(source)
            is OrderRaribleV2DataV3Sell -> RaribleV2DataV3DtoConverter.convert(source)
            is OrderX2Y2DataV1 -> X2Y2DataDtoConverter.convert(source)
            is OrderLooksrareDataV1 -> LooksrareDataDtoConverter.convert(source)
            is OrderSudoSwapAmmDataV1 -> SudoSwapAmmDataV1DtoConverter.convert(source)
            is OrderLooksrareDataV2 -> LooksrareDataDtoConverter.convert(source)
        }
    }
}
