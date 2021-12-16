package com.rarible.protocol.nft.core.service.ownership.reduce.status

import com.rarible.blockchain.scanner.framework.model.Log
import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipEvent
import com.rarible.protocol.nft.core.service.ownership.reduce.forward.ForwardChainOwnershipReducer
import com.rarible.protocol.nft.core.service.ownership.reduce.inactive.InactiveChainOwnershipReducer
import com.rarible.protocol.nft.core.service.ownership.reduce.pending.PendingChainOwnershipReducer
import com.rarible.protocol.nft.core.service.ownership.reduce.reversed.ReversedChainOwnershipReducer
import org.springframework.stereotype.Component

@Component
class EventStatusOwnershipReducer(
    private val forwardOwnershipReducer: ForwardChainOwnershipReducer,
    private val pendingOwnershipReducer: PendingChainOwnershipReducer,
    private val reversedOwnershipReducer: ReversedChainOwnershipReducer,
    private val inactiveOwnershipReducer: InactiveChainOwnershipReducer
) : Reducer<OwnershipEvent, Ownership> {

    override suspend fun reduce(entity: Ownership, event: OwnershipEvent): Ownership {
        return when (event.log.status) {
            Log.Status.CONFIRMED -> forwardOwnershipReducer.reduce(entity, event)
            Log.Status.PENDING -> pendingOwnershipReducer.reduce(entity, event)
            Log.Status.REVERTED -> reversedOwnershipReducer.reduce(entity, event)
            Log.Status.INACTIVE,
            Log.Status.DROPPED -> inactiveOwnershipReducer.reduce(entity, event)
        }
    }
}
