package com.rarible.protocol.nftorder.listener.service

import com.rarible.core.common.nowMillis
import com.rarible.core.common.optimisticLock
import com.rarible.protocol.dto.NftDeletedOwnershipDto
import com.rarible.protocol.dto.NftOwnershipDto
import com.rarible.protocol.dto.OrderDto
import com.rarible.protocol.nftorder.core.converter.NftOwnershipDtoConverter
import com.rarible.protocol.nftorder.core.converter.OwnershipToDtoConverter
import com.rarible.protocol.nftorder.core.converter.ShortOrderConverter
import com.rarible.protocol.nftorder.core.data.EnrichmentDataVerifier
import com.rarible.protocol.nftorder.core.event.OwnershipEventDelete
import com.rarible.protocol.nftorder.core.event.OwnershipEventListener
import com.rarible.protocol.nftorder.core.event.OwnershipEventUpdate
import com.rarible.protocol.nftorder.core.model.Ownership
import com.rarible.protocol.nftorder.core.model.OwnershipId
import com.rarible.protocol.nftorder.core.service.OrderService
import com.rarible.protocol.nftorder.core.service.OwnershipService
import com.rarible.protocol.nftorder.core.util.spent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class OwnershipEventService(
    private val ownershipService: OwnershipService,
    private val orderService: OrderService,
    private val itemEventService: ItemEventService,
    private val ownershipEventListeners: List<OwnershipEventListener>,
    private val bestOrderService: BestOrderService
) {
    private val logger = LoggerFactory.getLogger(OwnershipEventService::class.java)

    suspend fun onOwnershipUpdated(nftOwnership: NftOwnershipDto) {
        val received = NftOwnershipDtoConverter.convert(nftOwnership)
        optimisticLock {
            val existing = ownershipService.get(received.id)
            if (existing != null) {
                // If we have Ownership in DB, it also means we have some enrichment data here -
                // so we're just replacing root data and keep enrich data the same
                val updated = received.copy(bestSellOrder = existing.bestSellOrder)
                val saved = updateOwnership(updated)
                notifyUpdate(saved)
            } else {
                // Otherwise, we just proxy original event
                notifyUpdate(received)
            }
        }
    }

    suspend fun onOwnershipBestSellOrderUpdated(ownershipId: OwnershipId, order: OrderDto, forced: Boolean = false) = optimisticLock {
        updateOrder(ownershipId, order) { ownership ->
            ownership.copy(bestSellOrder = if (forced) ShortOrderConverter.convert(order) else bestOrderService.getBestSellOrder(ownership, order))
        }
    }

    suspend fun updateOrder(ownershipId: OwnershipId, order: OrderDto, orderUpdateAction: suspend (ownership: Ownership) -> Ownership) = optimisticLock {
        val fetchedItem = ownershipService.getOrFetchOwnershipById(ownershipId)
        val ownership = fetchedItem.entity
        val updated = orderUpdateAction(ownership)
        if (EnrichmentDataVerifier.isOwnershipNotEmpty(updated)) {
            // If order is completely the same - do nothing
            if (updated.bestSellOrder != ownership.bestSellOrder) {
                val saved = ownershipService.save(updated)
                notifyUpdate(saved, order)
                itemEventService.onOwnershipUpdated(ownershipId, order)
            }
        } else if (!fetchedItem.isFetched()) {
            logger.info("Deleting Ownership [{}] without related bestSellOrder", ownershipId)
            ownershipService.delete(ownershipId)
            notifyUpdate(updated, order)
            itemEventService.onOwnershipUpdated(ownershipId, order)
        }
    }

    suspend fun onOwnershipDeleted(nftOwnership: NftDeletedOwnershipDto) {
        val ownershipId = OwnershipId.parseId(nftOwnership.id)
        logger.debug("Deleting Ownership [{}] since it was removed from NFT-Indexer", ownershipId)
        val deleted = deleteOwnership(ownershipId)
        notifyDelete(ownershipId)
        if (deleted) {
            logger.info("Ownership [{}] deleted (removed from NFT-Indexer), refreshing sell stats", ownershipId)
            itemEventService.onOwnershipUpdated(ownershipId, null)
        }
    }

    private suspend fun updateOwnership(updated: Ownership): Ownership {
        val now = nowMillis()
        val result = ownershipService.save(updated)
        logger.info(
            "Updating Ownership [{}] with enrichment data: bestSellOrder = [{}] ({}ms)",
            updated.id, updated.bestSellOrder?.hash, spent(now)
        )
        return result
    }

    private suspend fun deleteOwnership(ownershipId: OwnershipId): Boolean {
        val result = ownershipService.delete(ownershipId)
        return result != null && result.deletedCount > 0
    }

    private suspend fun notifyDelete(ownershipId: OwnershipId) {
        val event = OwnershipEventDelete(ownershipId)
        ownershipEventListeners.forEach { it.onEvent(event) }
    }

    private suspend fun notifyUpdate(ownership: Ownership, order: OrderDto? = null) {
        val bestSellOrder = orderService.fetchOrderIfDiffers(ownership.bestSellOrder, order)

        val orders = listOfNotNull(bestSellOrder)
            .associateBy { it.hash }

        val dto = OwnershipToDtoConverter.convert(ownership, orders)
        val event = OwnershipEventUpdate(dto)
        ownershipEventListeners.forEach { it.onEvent(event) }
    }
}
