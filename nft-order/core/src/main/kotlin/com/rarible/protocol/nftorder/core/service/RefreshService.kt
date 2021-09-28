package com.rarible.protocol.nftorder.core.service

import com.rarible.core.common.optimisticLock
import com.rarible.protocol.dto.NftOwnershipDto
import com.rarible.protocol.nftorder.core.converter.*
import com.rarible.protocol.nftorder.core.data.EnrichmentDataVerifier
import com.rarible.protocol.nftorder.core.event.ItemEventListener
import com.rarible.protocol.nftorder.core.event.ItemEventUpdate
import com.rarible.protocol.nftorder.core.event.OwnershipEventListener
import com.rarible.protocol.nftorder.core.event.OwnershipEventUpdate
import com.rarible.protocol.nftorder.core.model.ItemId
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class RefreshService(
    private val itemService: ItemService,
    private val ownershipService: OwnershipService,
    private val orderService: OrderService,
    private val lockService: LockService,
    private val itemEventListeners: List<ItemEventListener>,
    private val ownershipEventListeners: List<OwnershipEventListener>
) {

    private val logger = LoggerFactory.getLogger(RefreshService::class.java)

    suspend fun refreshItemWithOwnerships(itemId: ItemId) = coroutineScope {
        logger.info("Starting full refresh of Item [{}] (with ownerships)", itemId.decimalStringValue)
        val ownerships = ownershipService.fetchAllByItemId(itemId)
        logger.info("Fetched {} Ownerships for Item [{}]", ownerships.size, itemId.decimalStringValue)
        ownerships
            .map { async { refreshOwnership(it) } }
            .map { it.await() }
        refreshItem(itemId)
    }

    suspend fun refreshItem(itemId: ItemId) = coroutineScope {
        logger.info("Starting refresh of Item [{}]", itemId.decimalStringValue)
        val nftItemDtoDeferred = async { itemService.fetchItemById(itemId) }
        val bestSellOrderDeferred = async { orderService.getBestSell(itemId) }
        val bestBidOrderDeferred = async { orderService.getBestBid(itemId) }
        val unlockableDeferred = async { lockService.isUnlockable(itemId) }
        val sellStats = ownershipService.getItemSellStats(itemId)

        val nftItemDto = nftItemDtoDeferred.await()
        val bestSellOrder = bestSellOrderDeferred.await()
        val bestBidOrder = bestBidOrderDeferred.await()

        val enrichedItem = NftItemDtoConverter.convert(nftItemDto).copy(
            bestBidOrder = bestBidOrderDeferred.await()?.let { ShortOrderConverter.convert(it) },
            bestSellOrder = bestSellOrderDeferred.await()?.let { ShortOrderConverter.convert(it) },
            unlockable = unlockableDeferred.await(),
            sellers = sellStats.sellers,
            totalStock = sellStats.totalStock
        )

        if (EnrichmentDataVerifier.isItemNotEmpty(enrichedItem)) {
            logger.info(
                "Saving refreshed Item [{}] with gathered enrichment data [{}]",
                itemId.decimalStringValue,
                enrichedItem
            )
            optimisticLock {
                val currentVersion = itemService.get(itemId)?.version
                itemService.save(enrichedItem.copy(version = currentVersion))
            }
        } else {
            logger.info("Item [{}] has no enrichment data: {}", itemId.decimalStringValue, enrichedItem)
            itemService.delete(itemId)
        }

        val orders = listOfNotNull(bestSellOrder, bestBidOrder)
            .associateBy { it.hash }

        val dto = ItemToDtoConverter.convert(enrichedItem, nftItemDto.meta!!, orders)
        val event = ItemEventUpdate(dto)

        itemEventListeners.forEach { it.onEvent(event) }
        dto
    }

    private suspend fun refreshOwnership(nftOwnershipDto: NftOwnershipDto) {
        val ownership = NftOwnershipDtoConverter.convert(nftOwnershipDto)
        val bestSellOrder = orderService.getBestSell(ownership.id)
        val enrichedOwnership = ownership.copy(
            bestSellOrder = bestSellOrder?.let { ShortOrderConverter.convert(it) }
        )

        if (EnrichmentDataVerifier.isOwnershipNotEmpty(enrichedOwnership)) {
            logger.info("Updating Ownership [{}] : {}", ownership.id.decimalStringValue, enrichedOwnership)
            ownershipService.save(enrichedOwnership)
        } else {
            val result = ownershipService.delete(ownership.id)
            // Nothing changed for this Ownership, event won't be sent
            if (result == null || result.deletedCount == 0L) {
                return
            }
        }

        val orders = listOfNotNull(bestSellOrder)
            .associateBy { it.hash }

        val dto = OwnershipToDtoConverter.convert(ownership, orders)
        val event = OwnershipEventUpdate(dto)

        ownershipEventListeners.forEach { it.onEvent(event) }
    }
}