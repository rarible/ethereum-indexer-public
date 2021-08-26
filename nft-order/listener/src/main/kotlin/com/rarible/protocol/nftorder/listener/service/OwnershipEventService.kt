package com.rarible.protocol.nftorder.listener.service

import com.rarible.core.common.convert
import com.rarible.core.common.nowMillis
import com.rarible.core.common.optimisticLock
import com.rarible.protocol.dto.NftDeletedOwnershipDto
import com.rarible.protocol.dto.NftOwnershipDto
import com.rarible.protocol.dto.OrderDto
import com.rarible.protocol.nftorder.core.data.EnrichmentDataVerifier
import com.rarible.protocol.nftorder.core.event.OwnershipEvent
import com.rarible.protocol.nftorder.core.event.OwnershipEventDelete
import com.rarible.protocol.nftorder.core.event.OwnershipEventListener
import com.rarible.protocol.nftorder.core.event.OwnershipEventUpdate
import com.rarible.protocol.nftorder.core.model.Ownership
import com.rarible.protocol.nftorder.core.model.OwnershipId
import com.rarible.protocol.nftorder.core.service.OwnershipService
import com.rarible.protocol.nftorder.core.util.spent
import org.slf4j.LoggerFactory
import org.springframework.core.convert.ConversionService
import org.springframework.stereotype.Component

@Component
class OwnershipEventService(
    private val conversionService: ConversionService,
    private val ownershipService: OwnershipService,
    private val itemEventService: ItemEventService,
    private val ownershipEventListeners: List<OwnershipEventListener>,
    private val bestOrderService: BestOrderService
) {
    private val logger = LoggerFactory.getLogger(OwnershipEventService::class.java)

    suspend fun onOwnershipUpdated(nftOwnership: NftOwnershipDto) {
        val received = conversionService.convert<Ownership>(nftOwnership)
        optimisticLock {
            val existing = ownershipService.get(received.id)
            if (existing != null) {
                // If we have Ownership in DB, it also means we have some enrichment data here -
                // so we're just replacing root data and keep enrich data the same
                val updated = received.copy(bestSellOrder = existing.bestSellOrder)
                val saved = updateOwnership(updated)
                notify(OwnershipEventUpdate(saved))
            } else {
                // Otherwise, we just proxy original event
                notify(OwnershipEventUpdate(received))
            }
        }
    }

    suspend fun onOwnershipBestSellOrderUpdated(ownershipId: OwnershipId, order: OrderDto) = optimisticLock {
        val fetchedItem = ownershipService.getOrFetchOwnershipById(ownershipId)
        val ownership = fetchedItem.entity
        val updated = ownership.copy(bestSellOrder = bestOrderService.getBestSellOrder(ownership, order))
        if (EnrichmentDataVerifier.isOwnershipNotEmpty(updated)) {
            // If order is completely the same - do nothing
            if (updated.bestSellOrder != ownership.bestSellOrder) {
                val saved = ownershipService.save(updated)
                notify(OwnershipEventUpdate(saved))
                itemEventService.onOwnershipUpdated(ownershipId)
            }
        } else if (!fetchedItem.isFetched) {
            logger.info("Deleting Ownership [{}] without related bestSellOrder", ownershipId)
            ownershipService.delete(ownershipId)
            notify(OwnershipEventUpdate(updated))
            itemEventService.onOwnershipUpdated(ownershipId)
        }
    }

    suspend fun onOwnershipDeleted(nftOwnership: NftDeletedOwnershipDto) {
        val ownershipId = OwnershipId.parseId(nftOwnership.id)
        logger.debug("Deleting Ownership [{}] since it was removed from NFT-Indexer", ownershipId)
        val deleted = deleteOwnership(ownershipId)
        notify(OwnershipEventDelete(ownershipId))
        if (deleted) {
            logger.info("Ownership [{}] deleted (removed from NFT-Indexer), refreshing sell stats", ownershipId)
            itemEventService.onOwnershipUpdated(ownershipId)
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

    private suspend fun notify(event: OwnershipEvent) {
        ownershipEventListeners.forEach { it.onEvent(event) }
    }
}