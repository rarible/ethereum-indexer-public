package com.rarible.protocol.nft.core.service.ownership.reduce

import com.rarible.core.entity.reducer.service.ReversedReducer
import com.rarible.protocol.nft.core.model.*
import com.rarible.protocol.nft.core.service.ownership.reduce.forward.ForwardChainOwnershipReducer
import com.rarible.protocol.nft.core.service.ownership.reduce.reversed.ReversedChainOwnershipReducer
import org.springframework.stereotype.Component

@Component
class BlockchainOwnershipReducer(
    private val ownershipReducer: ForwardChainOwnershipReducer,
    private val reversedOwnershipReducer: ReversedChainOwnershipReducer
) : ReversedReducer<OwnershipEvent, Ownership> {

    override suspend fun reduce(entity: Ownership, event: OwnershipEvent): Ownership {
        return when (event.status) {
            BlockchainEntityEvent.Status.CONFIRMED,
            BlockchainEntityEvent.Status.PENDING -> {
                ownershipReducer.reduce(entity, event)
            }
            BlockchainEntityEvent.Status.REVERTED -> {
                reversedOwnershipReducer.reduce(entity, event)
            }
        }
    }
}
