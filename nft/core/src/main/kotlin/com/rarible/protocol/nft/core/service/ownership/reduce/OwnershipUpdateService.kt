package com.rarible.protocol.nft.core.service.ownership.reduce

import com.rarible.core.entity.reducer.service.EntityService
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipId
import com.rarible.protocol.nft.core.repository.ownership.OwnershipRepository
import com.rarible.protocol.nft.core.service.item.ReduceEventListenerListener
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class OwnershipUpdateService(
    private val ownershipRepository: OwnershipRepository,
    private val eventListenerListener: ReduceEventListenerListener
) : EntityService<OwnershipId, Ownership> {

    override suspend fun get(id: OwnershipId): Ownership? {
        return ownershipRepository.findById(id).awaitFirstOrNull()
    }

    override suspend fun update(entity: Ownership): Ownership {
        val savedOwnership = ownershipRepository.save(entity).awaitFirst()
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
            append("revertableEvents=${ownership.revertableEvents}, ")
        })
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OwnershipUpdateService::class.java)
    }
}
