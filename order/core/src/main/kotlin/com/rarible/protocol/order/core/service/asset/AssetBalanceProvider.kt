package com.rarible.protocol.order.core.service.asset

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.misc.ownershipId
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.CollectionAssetType
import com.rarible.protocol.order.core.model.CryptoPunksAssetType
import com.rarible.protocol.order.core.model.Erc1155AssetType
import com.rarible.protocol.order.core.model.Erc1155LazyAssetType
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.Erc721LazyAssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.GenerativeArtAssetType
import com.rarible.protocol.order.core.service.balance.BalanceControllerApiService
import com.rarible.protocol.order.core.service.balance.EthBalanceService
import com.rarible.protocol.order.core.service.nft.NftOwnershipApiService
import org.springframework.stereotype.Component
import scalether.domain.Address

interface AssetBalanceProvider {
    suspend fun getAssetStock(owner: Address, asset: Asset): EthUInt256?
}

@Component
@CaptureSpan(type = SpanType.EXT)
class AssetBalanceProviderImpl(
    private val erc20BalanceApi: BalanceControllerApiService,
    private val nftOwnershipApi: NftOwnershipApiService,
    private val ethBalanceService: EthBalanceService
) : AssetBalanceProvider {
    override suspend fun getAssetStock(owner: Address, asset: Asset): EthUInt256? {
        return when (asset.type) {
            is Erc20AssetType -> {
                erc20BalanceApi
                    .getBalance(asset.type.token, owner)
                    ?.let { balance -> EthUInt256.of(balance.balance) }
            }
            is Erc721AssetType, is Erc1155AssetType, is CryptoPunksAssetType -> {
                val ownershipId = asset.type.ownershipId(owner)
                nftOwnershipApi
                    .getOwnershipById(ownershipId)
                    ?.let { ownership -> EthUInt256.of(ownership.value) }
            }
            is Erc721LazyAssetType -> {
                EthUInt256.ONE
            }
            is GenerativeArtAssetType -> {
                EthUInt256.of(Long.MAX_VALUE)
            }
            is CollectionAssetType -> {
                EthUInt256.of(Long.MAX_VALUE)
            }
            is Erc1155LazyAssetType -> {
                asset.type.supply
            }
            is EthAssetType -> {
                ethBalanceService.getBalance(owner)
            }
        }
    }
}
