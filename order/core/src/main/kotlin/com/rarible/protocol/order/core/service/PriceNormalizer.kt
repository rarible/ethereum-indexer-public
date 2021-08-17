package com.rarible.protocol.order.core.service

import com.rarible.core.contract.model.Erc20Token
import com.rarible.ethereum.contract.service.ContractService
import com.rarible.protocol.order.core.model.*
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class PriceNormalizer(
    private val contractService: ContractService
) {
    suspend fun decimals(asset: Asset): Int {
        return when (val assetType = asset.type) {
            is Erc1155AssetType -> 0
            is Erc1155LazyAssetType -> 0
            is Erc20AssetType -> (contractService.get(assetType.token) as Erc20Token).decimals ?: 0
            is GenerativeArtAssetType -> 0
            is Erc721AssetType -> 0
            is Erc721LazyAssetType -> 0
            is CryptoPunksAssetType -> 0
            is EthAssetType -> 18
        }
    }

    suspend fun normalize(asset: Asset): BigDecimal {
        return asset.value.value.toBigDecimal(decimals(asset))
    }
}
