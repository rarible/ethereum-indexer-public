package com.rarible.protocol.nft.core.service.ownership.reduce.pending

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipEvent
import org.springframework.stereotype.Component

@Component
class PendingOwnershipValueReducer : Reducer<OwnershipEvent, Ownership> {
    override suspend fun reduce(entity: Ownership, event: OwnershipEvent): Ownership {
        return when (event) {
            is OwnershipEvent.TransferToEvent,
            is OwnershipEvent.TransferFromEvent,
            is OwnershipEvent.ChangeLazyValueEvent, -> entity
            is OwnershipEvent.LazyTransferToEvent ->
                throw IllegalArgumentException("This events can't be in this reducer")
        }
    }
}
