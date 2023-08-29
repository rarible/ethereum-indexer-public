package com.rarible.protocol.nft.core.service.ownership.reduce

import com.rarible.core.entity.reducer.service.EntityService
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.misc.nftOffchainEventMarks
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipContinuation
import com.rarible.protocol.nft.core.model.OwnershipEvent
import com.rarible.protocol.nft.core.model.OwnershipFilter
import com.rarible.protocol.nft.core.model.OwnershipFilterByItem
import com.rarible.protocol.nft.core.model.OwnershipId
import com.rarible.protocol.nft.core.service.item.ItemReduceEventListener
import com.rarible.protocol.nft.core.service.ownership.OwnershipService
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class OwnershipUpdateService(
    private val ownershipService: OwnershipService,
    private val eventListenerListener: ItemReduceEventListener,
    properties: NftIndexerProperties,
) : EntityService<OwnershipId, Ownership, OwnershipEvent> {

    private val fetchSize = properties.ownershipFetchBatchSize

    override suspend fun get(id: OwnershipId): Ownership? {
        return ownershipService.get(id).awaitFirstOrNull()
    }

    override suspend fun getAll(ids: Collection<OwnershipId>): List<Ownership> {
        return ownershipService.getAll(ids)
    }

    override suspend fun update(entity: Ownership, event: OwnershipEvent?): Ownership {
        val eventTimeMarks = event?.eventTimeMarks ?: nftOffchainEventMarks()
        val savedOwnership = ownershipService.save(entity)
        eventListenerListener.onOwnershipChanged(savedOwnership, eventTimeMarks).awaitFirstOrNull()
        logUpdatedOwnership(savedOwnership)
        return savedOwnership
    }

    /**
     * Marks all ownerships of given ItemId as deleted and sends corresponding event
     */
    suspend fun deleteByItemId(itemId: ItemId) {
        var continuation: OwnershipContinuation? = null
        do {
            val filter = OwnershipFilterByItem(
                contract = itemId.token,
                tokenId = itemId.tokenId.value,
                sort = OwnershipFilter.Sort.LAST_UPDATE
            )
            val ownerships = ownershipService.search(filter, continuation, fetchSize)
            continuation = if (ownerships.size < fetchSize) null else ownerships.last()
                .let { OwnershipContinuation(it.date, it.id) }
            ownerships.forEach {
                update(it.copy(deleted = true))
            }
        } while (continuation != null)
    }

    private fun logUpdatedOwnership(ownership: Ownership) {
        logger.info(buildString {
            append("Updated ownership: ")
            append("id=${ownership.id}, ")
            append("value=${ownership.value}, ")
            append("lazyValue=${ownership.lazyValue}, ")
            append("lastLazyEventTimestamp=${ownership.lastLazyEventTimestamp}, ")
            append("deleted=${ownership.deleted}, ")
            append("last revertableEvents=${ownership.revertableEvents.lastOrNull()}, ")
        })
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OwnershipUpdateService::class.java)
    }
}
