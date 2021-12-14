package com.rarible.protocol.nft.core.service.ownership.reduce.forward

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipEvent
import org.springframework.stereotype.Component

@Component
class ForwardOwnershipValueReducer : Reducer<OwnershipEvent, Ownership> {
    override suspend fun reduce(entity: Ownership, event: OwnershipEvent): Ownership {
        val value = when (event) {
            is OwnershipEvent.TransferToEvent -> entity.value + event.value
            is OwnershipEvent.TransferFromEvent -> entity.value - event.value
            is OwnershipEvent.ChangeLazyValueEvent -> entity.value
            is OwnershipEvent.LazyTransferToEvent ->
                throw IllegalArgumentException("This events can't be in this reducer")
        }
        return entity.copy(value = value)
    }
}

