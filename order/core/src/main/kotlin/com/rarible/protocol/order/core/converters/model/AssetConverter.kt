package com.rarible.protocol.order.core.converters.model

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.AssetDto
import com.rarible.protocol.order.core.model.Asset
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
object AssetConverter : Converter<AssetDto, Asset> {
    override fun convert(source: AssetDto): Asset {
        return Asset(AssetTypeConverter.convert(source.assetType), EthUInt256.of(source.value))
    }
}
