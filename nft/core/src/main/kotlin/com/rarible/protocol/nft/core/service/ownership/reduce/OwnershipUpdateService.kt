package com.rarible.protocol.nft.core.service.ownership.reduce

import com.rarible.core.entity.reducer.service.EntityService
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipId
import com.rarible.protocol.nft.core.repository.ownership.OwnershipRepository
import com.rarible.protocol.nft.core.service.item.ReduceEventListenerListener
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
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
        val savedOwnership = ownershipRepository.save(entity.withCalculated()).awaitFirst()
        eventListenerListener.onOwnershipChanged(savedOwnership).awaitFirstOrNull()
        return savedOwnership
    }
}
