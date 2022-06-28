package com.rarible.protocol.order.core.service

import com.rarible.core.contract.model.Erc20Token
import com.rarible.ethereum.contract.service.ContractService
import com.rarible.protocol.order.core.model.*
import org.springframework.stereotype.Component
import scalether.domain.Address
import java.math.BigDecimal
import java.math.BigInteger
import java.util.concurrent.ConcurrentHashMap

@Component
class PriceNormalizer(
    private val contractService: ContractService
) {

    // There are not a lot of Erc20 contracts and they can't be updated in runtime,
    // so we can store decimal count in cache in order to avoid DB access on each call
    private val erc20Cache = ConcurrentHashMap<Address, Int>()

    suspend fun normalize(asset: Asset): BigDecimal {
        return normalize(asset.type, asset.value.value)
    }

    suspend fun normalize(assetType: AssetType, value: BigInteger): BigDecimal {
        return value.toBigDecimal(decimals(assetType))
    }

    private suspend fun decimals(assetType: AssetType): Int {
        return when (assetType) {
            is EthAssetType -> 18
            is Erc20AssetType -> getErc20Decimals(assetType.token)
            is Erc1155AssetType -> 0
            is Erc1155LazyAssetType -> 0
            is Erc721AssetType -> 0
            is Erc721LazyAssetType -> 0
            is CryptoPunksAssetType -> 0
            is GenerativeArtAssetType -> 0
            is CollectionAssetType -> 0
        }
    }

    suspend fun getErc20Decimals(token: Address): Int {
        var result = erc20Cache[token]
        if (result == null) {
            result = (contractService.get(token) as Erc20Token).decimals ?: 0
            erc20Cache[token] = result
        }
        return result
    }

}
