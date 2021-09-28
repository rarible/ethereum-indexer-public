package com.rarible.protocol.nftorder.listener.service

import com.rarible.core.common.nowMillis
import com.rarible.core.common.optimisticLock
import com.rarible.protocol.dto.NftItemDto
import com.rarible.protocol.dto.NftItemMetaDto
import com.rarible.protocol.dto.OrderDto
import com.rarible.protocol.nftorder.core.converter.NftItemDtoConverter
import com.rarible.protocol.nftorder.core.data.EnrichmentDataVerifier
import com.rarible.protocol.nftorder.core.data.ItemSellStats
import com.rarible.protocol.nftorder.core.event.ItemEventDelete
import com.rarible.protocol.nftorder.core.event.ItemEventListener
import com.rarible.protocol.nftorder.core.event.ItemEventUpdate
import com.rarible.protocol.nftorder.core.model.Item
import com.rarible.protocol.nftorder.core.model.ItemId
import com.rarible.protocol.nftorder.core.model.OwnershipId
import com.rarible.protocol.nftorder.core.service.ItemService
import com.rarible.protocol.nftorder.core.service.OrderService
import com.rarible.protocol.nftorder.core.service.OwnershipService
import com.rarible.protocol.nftorder.core.util.spent
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ItemEventService(
    private val itemService: ItemService,
    private val ownershipService: OwnershipService,
    private val orderService: OrderService,
    private val itemEventListeners: List<ItemEventListener>,
    private val bestOrderService: BestOrderService
) {

    private val logger = LoggerFactory.getLogger(ItemEventService::class.java)

    // If ownership was updated, we need to recalculate totalStock/sellers for related item,
    // also, we can specify here Order which triggered this update - ItemService
    // can use this full Order to avoid unnecessary getOrderById calls
    suspend fun onOwnershipUpdated(ownershipId: OwnershipId, order: OrderDto?) {
        val itemId = ItemId(ownershipId.token, ownershipId.tokenId)
        optimisticLock {
            val item = itemService.get(itemId)
            if (item == null) {
                logger.debug(
                    "Item [{}] not found in DB, skipping sell stats update on Ownership event: [{}]",
                    itemId, ownershipId
                )
            } else {
                val refreshedSellStats = ownershipService.getItemSellStats(itemId)
                val currentSellStats = ItemSellStats(item.sellers, item.totalStock)
                if (refreshedSellStats != currentSellStats) {
                    val updatedItem = item.copy(
                        sellers = refreshedSellStats.sellers,
                        totalStock = refreshedSellStats.totalStock
                    )
                    logger.info(
                        "Updating Item [{}] with new sell stats, was [{}] , now: [{}]",
                        itemId, currentSellStats, refreshedSellStats
                    )
                    val saved = itemService.save(updatedItem)
                    notifyUpdate(saved, null, order)
                } else {
                    logger.debug(
                        "Sell stats of Item [{}] are the same as before Ownership event [{}], skipping update",
                        itemId, ownershipId
                    )
                }
            }
        }
    }

    suspend fun onItemUpdated(nftItem: NftItemDto) {
        val received = NftItemDtoConverter.convert(nftItem)
        optimisticLock {
            val existing = itemService.get(received.id)
            if (existing != null) {
                // If we have Item in DB, it also means we have some enrichment data here -
                // so we're just replacing root data and keep enrich data the same
                val updated = received.copy(
                    bestBidOrder = existing.bestBidOrder,
                    bestSellOrder = existing.bestSellOrder,
                    totalStock = existing.totalStock,
                    sellers = existing.sellers,
                    unlockable = existing.unlockable
                )
                val saved = updateItem(existing, updated)
                notifyUpdate(saved, nftItem.meta)
            } else {
                // Otherwise, we just proxy original event
                notifyUpdate(received, nftItem.meta)
            }
        }
    }

    suspend fun onItemBestSellOrderUpdated(itemId: ItemId, order: OrderDto) {
        updateOrder(itemId, order) { item ->
            item.copy(bestSellOrder = bestOrderService.getBestSellOrder(item, order))
        }
    }

    suspend fun onItemBestBidOrderUpdated(itemId: ItemId, order: OrderDto) {
        updateOrder(itemId, order) { item ->
            item.copy(bestBidOrder = bestOrderService.getBestBidOrder(item, order))
        }
    }

    suspend fun onForceItemBestSellOrderUpdate(itemId: ItemId, order: OrderDto) {
        updateOrder(itemId, order) { item ->
            val withDroppedBestSellOrderItem = item.copy(bestSellOrder = null)
            val bestSellOrder = bestOrderService.getBestSellOrder(withDroppedBestSellOrderItem, order)
            item.copy(bestSellOrder = bestSellOrder)
        }
    }

    suspend fun onForceItemBestBidOrderUpdate(itemId: ItemId, order: OrderDto) {
        updateOrder(itemId, order) { item ->
            val withDroppedBestBidOrderItem = item.copy(bestBidOrder = null)
            val bestBidOrder = bestOrderService.getBestBidOrder(withDroppedBestBidOrderItem, order)
            item.copy(bestBidOrder = bestBidOrder)
        }
    }

    private suspend fun updateOrder(itemId: ItemId, order: OrderDto, orderUpdateAction: suspend (item: Item) -> Item) {
        optimisticLock {
            val fetchedItem = itemService.getOrFetchItemById(itemId)
            val item = fetchedItem.entity
            val updated = orderUpdateAction(item)
            if (item != updated) {
                if (EnrichmentDataVerifier.isItemNotEmpty(updated)) {
                    val saved = itemService.save(updated)
                    notifyUpdate(saved, fetchedItem.original?.meta, order)
                } else if (!fetchedItem.isFetched()) {
                    itemService.delete(itemId)
                    logger.info("Deleted Item [{}] without enrichment data", itemId)
                    notifyUpdate(updated, null, order)
                }
            } else {
                logger.info("Item [{}] not changed after order updated, event won't be published", itemId)
            }
        }
    }

    private suspend fun updateItem(existing: Item, updated: Item): Item {
        val now = nowMillis()
        val result = itemService.save(updated.copy(version = existing.version))
        logger.info(
            "Updated Item [{}] with data: totalStock = {}, sellers = {}, bestSellOrder = [{}], bestBidOrder = [{}], unlockable = [{}] ({}ms)",
            updated.id,
            updated.totalStock,
            updated.sellers,
            updated.bestSellOrder?.hash,
            updated.bestBidOrder?.hash,
            updated.unlockable,
            spent(now)
        )
        return result
    }

    suspend fun onItemDeleted(itemId: ItemId) {
        val deleted = deleteItem(itemId)
        notifyDelete(itemId)
        if (deleted) {
            logger.info("Item [{}] deleted (removed from NFT-Indexer)", itemId)
        }
    }

    private suspend fun deleteItem(itemId: ItemId): Boolean {
        val result = itemService.delete(itemId)
        return result != null && result.deletedCount > 0
    }

    suspend fun onLockCreated(itemId: ItemId) {
        logger.info("Updating Item [{}] marked as Unlockable", itemId)
        val fetched = itemService.getOrFetchItemById(itemId)
        val item = fetched.entity.copy(unlockable = true)
        val saved = itemService.save(item)
        notifyUpdate(saved, fetched.original?.meta, null)
    }

    private suspend fun notifyDelete(itemId: ItemId) {
        val event = ItemEventDelete(itemId)
        itemEventListeners.forEach { it.onEvent(event) }
    }

    // Potentially we could have updated Order here (no matter - bid/sell) and when we need to fetch
    // full version of the order, we can use this already fetched Order if it has same ID (hash)
    private suspend fun notifyUpdate(item: Item, meta: NftItemMetaDto?, order: OrderDto? = null) = coroutineScope {
        val dto = itemService.enrichItem(item, meta, order)
        val event = ItemEventUpdate(dto)
        itemEventListeners.forEach { it.onEvent(event) }
    }
}
