package com.rarible.protocol.order.core.converters.dto

import com.rarible.protocol.dto.OrderFormAssetDto
import com.rarible.protocol.order.core.model.Asset

object OrderFormAssetDtoConverter {

    fun convert(source: Asset): OrderFormAssetDto =
        OrderFormAssetDto(
            assetType = AssetTypeDtoConverter.convert(source.type),
            value = source.value.value
        )
}
