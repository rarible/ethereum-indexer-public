package com.rarible.protocol.order.core.converters.dto

import com.rarible.protocol.dto.AssetDto
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.service.PriceNormalizer
import org.springframework.stereotype.Component

@Component
class AssetDtoConverter(
    private val priceNormalizer: PriceNormalizer
) {

    suspend fun convert(source: Asset): AssetDto =
        AssetDto(
            assetType = AssetTypeDtoConverter.convert(source.type),
            value = source.value.value,
            valueDecimal = priceNormalizer.normalize(source)
        )
}
