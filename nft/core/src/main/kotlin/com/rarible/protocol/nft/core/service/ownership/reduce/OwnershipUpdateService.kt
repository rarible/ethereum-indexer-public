package com.rarible.protocol.nft.core.service.ownership.reduce

import com.rarible.core.entity.reducer.service.EntityService
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipId
import com.rarible.protocol.nft.core.service.item.ReduceEventListenerListener
import com.rarible.protocol.nft.core.service.ownership.OwnershipService
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class OwnershipUpdateService(
    private val ownershipService: OwnershipService,
    private val eventListenerListener: ReduceEventListenerListener
) : EntityService<OwnershipId, Ownership> {

    override suspend fun get(id: OwnershipId): Ownership? {
        return ownershipService.get(id).awaitFirstOrNull()
    }

    override suspend fun update(entity: Ownership): Ownership {
        // Cleanup of deprecated data from ReducerV1, replaced by revertableEvents
        val cleanedUp = entity.copy(pending = emptyList())

        val savedOwnership = ownershipService.save(cleanedUp)
        eventListenerListener.onOwnershipChanged(savedOwnership).awaitFirstOrNull()

        logUpdatedOwnership(savedOwnership)
        return savedOwnership
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
