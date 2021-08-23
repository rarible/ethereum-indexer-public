package com.rarible.protocol.nftorder.listener.service

import com.rarible.core.common.convert
import com.rarible.core.common.optimisticLock
import com.rarible.protocol.dto.NftDeletedOwnershipDto
import com.rarible.protocol.dto.NftOwnershipDto
import com.rarible.protocol.dto.OrderDto
import com.rarible.protocol.nftorder.core.data.OwnershipEnrichmentData
import com.rarible.protocol.nftorder.core.event.OwnershipEvent
import com.rarible.protocol.nftorder.core.event.OwnershipEventDelete
import com.rarible.protocol.nftorder.core.event.OwnershipEventListener
import com.rarible.protocol.nftorder.core.event.OwnershipEventUpdate
import com.rarible.protocol.nftorder.core.model.Ownership
import com.rarible.protocol.nftorder.core.model.OwnershipId
import com.rarible.protocol.nftorder.core.service.OwnershipService
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
        val rawOwnership = conversionService.convert<Ownership>(nftOwnership)
        val enrichmentData = ownershipService.getEnrichmentData(rawOwnership.id)
        onOwnershipUpdated(rawOwnership, enrichmentData)
    }

    suspend fun onOwnershipUpdated(rawOwnership: Ownership, data: OwnershipEnrichmentData) {
        val updated = ownershipService.enrichOwnership(rawOwnership, data)
        optimisticLock {
            val existing = ownershipService.get(updated.id)
            if (data.isNotEmpty()) {
                updateOwnership(existing, updated, data)
            } else if (existing != null) {
                deleteOwnership(rawOwnership.id)
            }
        }
        notify(OwnershipEventUpdate(updated))
        itemEventService.onOwnershipUpdated(rawOwnership.id)
    }

    suspend fun onOwnershipBestSellOrderUpdated(ownershipId: OwnershipId, order: OrderDto) {
        val ownership = optimisticLock {
            val fetchedItem = ownershipService.getOrFetchOwnershipById(ownershipId)
            val ownership = fetchedItem.entity
            val updated = ownership.copy(bestSellOrder = bestOrderService.getBestSellOrder(ownership, order))
            if (OwnershipEnrichmentData.isNotEmpty(updated)) {
                ownershipService.save(updated)
            } else if (!fetchedItem.isFetched) {
                deleteOwnership(ownershipId)
            }
            updated
        }
        notify(OwnershipEventUpdate(ownership))
        itemEventService.onOwnershipUpdated(ownershipId)
    }

    suspend fun onOwnershipDeleted(nftOwnership: NftDeletedOwnershipDto) {
        val ownershipId = OwnershipId.parseId(nftOwnership.id)
        logger.info("Deleting Ownership [{}] since it was removed from NFT-Indexer", ownershipId)
        deleteOwnership(ownershipId)
        notify(OwnershipEventDelete(ownershipId))
        itemEventService.onOwnershipUpdated(ownershipId)
    }

    private suspend fun updateOwnership(existing: Ownership?, updated: Ownership, data: OwnershipEnrichmentData) {
        if (existing == null) {
            logger.info(
                "Inserting Ownership [{}] with enrichment data: bestSellOrder = [{}]",
                updated.id, data.bestSellOrder?.hash
            )
        } else {
            logger.info(
                "Updating Ownership [{}] with enrichment data: bestSellOrder = [{}]",
                updated.id, data.bestSellOrder?.hash
            )
        }
        ownershipService.save(updated)
    }

    private suspend fun deleteOwnership(ownershipId: OwnershipId) {
        val result = ownershipService.delete(ownershipId)
        if (result != null && result.deletedCount > 0) {
            logger.info("Deleted Ownership [{}] without related bestSellOrder", ownershipId)
        }
    }

    private suspend fun notify(event: OwnershipEvent) {
        ownershipEventListeners.forEach { it.onEvent(event) }
    }
}