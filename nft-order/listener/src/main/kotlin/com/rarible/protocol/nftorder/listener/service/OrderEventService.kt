package com.rarible.protocol.nftorder.listener.service

import com.rarible.core.client.WebClientResponseProxyException
import com.rarible.protocol.dto.AssetTypeDto
import com.rarible.protocol.dto.CollectionAssetTypeDto
import com.rarible.protocol.dto.CryptoPunksAssetTypeDto
import com.rarible.protocol.dto.Erc1155AssetTypeDto
import com.rarible.protocol.dto.Erc1155LazyAssetTypeDto
import com.rarible.protocol.dto.Erc20AssetTypeDto
import com.rarible.protocol.dto.Erc721AssetTypeDto
import com.rarible.protocol.dto.Erc721LazyAssetTypeDto
import com.rarible.protocol.dto.EthAssetTypeDto
import com.rarible.protocol.dto.GenerativeArtAssetTypeDto
import com.rarible.protocol.dto.OrderDto
import com.rarible.protocol.nftorder.core.model.ItemId
import com.rarible.protocol.nftorder.core.model.MissedCollection
import com.rarible.protocol.nftorder.core.model.OwnershipId
import com.rarible.protocol.nftorder.core.repository.MissedCollectionRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class OrderEventService(
    private val itemEventService: ItemEventService,
    private val ownershipEventService: OwnershipEventService,
    private val missedCollectionRepository: MissedCollectionRepository
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun updateOrder(order: OrderDto, forced: Boolean = false) = coroutineScope {

        val makeItemId = toItemId(order.make.assetType)
        val takeItemId = toItemId(order.take.assetType)

        val mFuture = makeItemId?.let {
            async { ignoreApi404(it) {
                itemEventService.onItemBestSellOrderUpdated(makeItemId, order, forced)
            } }
        }
        val tFuture = takeItemId?.let {
            async { ignoreApi404(it) {
                itemEventService.onItemBestBidOrderUpdated(takeItemId, order, forced)
            } }
        }
        val oFuture = makeItemId?.let {
            val ownershipId = OwnershipId(makeItemId.token, makeItemId.tokenId, order.maker)
            async { ignoreApi404(it) {
                ownershipEventService.onOwnershipBestSellOrderUpdated(ownershipId, order, forced)
            } }
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
            is CryptoPunksAssetTypeDto -> ItemId.of(assetType.contract, assetType.tokenId.toBigInteger())
            is GenerativeArtAssetTypeDto -> null
            is CollectionAssetTypeDto -> null
            is EthAssetTypeDto -> null
            is Erc20AssetTypeDto -> null
        }
    }

    private suspend fun ignoreApi404(itemId: ItemId, call: suspend () -> Unit) {
        try {
            call()
        } catch (ex: WebClientResponseProxyException) {
            logger.warn("Received NOT_FOUND code from client, details: {}, message: {}", ex.data, ex.message)
            missedCollectionRepository.save(MissedCollection(itemId.token))
        }
    }
}
