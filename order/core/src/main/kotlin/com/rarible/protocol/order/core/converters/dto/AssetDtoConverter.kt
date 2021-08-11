package com.rarible.protocol.order.core.converters.dto

import com.rarible.protocol.dto.AssetDto
import com.rarible.protocol.order.core.model.Asset
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
object AssetDtoConverter : Converter<Asset, AssetDto> {
    override fun convert(source: Asset): AssetDto =
        AssetDto(
            AssetTypeDtoConverter.convert(source.type),
            value = source.value.value
        )
}
