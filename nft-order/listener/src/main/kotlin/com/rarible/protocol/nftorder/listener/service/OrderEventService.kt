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
        val (makeAssetType, takeAssetType) = when (order) {
            is RaribleV2OrderDto -> order.make.assetType to order.take.assetType
            is OpenSeaV1OrderDto -> order.make.assetType to order.take.assetType
            is LegacyOrderDto -> order.make.assetType to order.take.assetType
            is CryptoPunkBidOrderDto -> EthAssetTypeDto() to order.punk
            is CryptoPunkSellOrderDto -> order.punk to EthAssetTypeDto()
        }
        val makeItemId = toItemId(makeAssetType)
        val takeItemId = toItemId(takeAssetType)

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
            is CryptoPunksAssetTypeDto -> ItemId.of(assetType.contract, assetType.punkId.toBigInteger())
            is EthAssetTypeDto -> null
            is FlowAssetTypeDto -> null
            is Erc20AssetTypeDto -> null
        }
    }

}