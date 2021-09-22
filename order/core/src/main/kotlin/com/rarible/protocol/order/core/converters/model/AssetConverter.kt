package com.rarible.protocol.order.core.converters.model

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.OrderFormAssetDto
import com.rarible.protocol.order.core.model.Asset
import org.springframework.stereotype.Component

@Component
object AssetConverter {
    fun convert(source: OrderFormAssetDto): Asset {
        return Asset(AssetTypeConverter.convert(source.assetType), EthUInt256.of(source.value))
    }
}
