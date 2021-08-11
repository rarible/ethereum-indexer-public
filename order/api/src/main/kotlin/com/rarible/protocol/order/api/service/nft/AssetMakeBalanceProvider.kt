package com.rarible.protocol.order.api.service.nft

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.model.AssetType
import com.rarible.protocol.order.core.service.asset.AssetBalanceProvider
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class AssetMakeBalanceProvider(
    private val delegate: AssetBalanceProvider
) {
    suspend fun getAssetStock(maker: Address, type: AssetType): EthUInt256 {
        return delegate.getAssetStock(maker, type) ?: EthUInt256.ZERO
    }
}