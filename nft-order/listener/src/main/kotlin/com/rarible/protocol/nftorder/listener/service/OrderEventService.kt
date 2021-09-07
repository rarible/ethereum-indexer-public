package com.rarible.protocol.nftorder.listener.service

import com.rarible.protocol.dto.*
import com.rarible.protocol.nftorder.core.model.ItemId
import com.rarible.protocol.nftorder.core.model.OwnershipId
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Component

@Component
class OrderEventService(
    private val itemEventService: ItemEventService,
    private val ownershipEventService: OwnershipEventService
) {

    suspend fun updateOrder(order: OrderDto) = coroutineScope {

        val makeItemId = toItemId(order.make.assetType)
        val takeItemId = toItemId(order.take.assetType)

        val mFuture = makeItemId?.let { async { itemEventService.onItemBestSellOrderUpdated(makeItemId, order) } }
        val tFuture = takeItemId?.let { async { itemEventService.onItemBestBidOrderUpdated(takeItemId, order) } }
        val oFuture = makeItemId?.let {
            async {
                val ownershipId = OwnershipId(makeItemId.token, makeItemId.tokenId, order.maker)
                ownershipEventService.onOwnershipBestSellOrderUpdated(ownershipId, order)
            }
        }

        mFuture?.await()
        tFuture?.await()
        oFuture?.await()
    }

    private fun toItemId(assetType: AssetTypeDto): ItemId? {

        return when (assetType) {
            is Erc721AssetTypeDto -> ItemId.of(assetType.contract, assetType.tokenId)
            is Erc1155AssetTypeDto -> ItemId.of(assetType.contract, assetType.tokenId)
            is Erc721LazyAssetTypeDto -> ItemId.of(assetType.contract, assetType.tokenId)
            is Erc1155LazyAssetTypeDto -> ItemId.of(assetType.contract, assetType.tokenId)
            is GenerativeArtAssetTypeDto -> null
            is EthAssetTypeDto -> null
            is FlowAssetTypeDto -> null
            is Erc20AssetTypeDto -> null
        }
    }

}