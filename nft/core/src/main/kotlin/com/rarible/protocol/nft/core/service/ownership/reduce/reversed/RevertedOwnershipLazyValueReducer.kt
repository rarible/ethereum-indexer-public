package com.rarible.protocol.nft.core.service.ownership.reduce.reversed

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipEvent
import org.springframework.stereotype.Component

@Component
class RevertedOwnershipLazyValueReducer : Reducer<OwnershipEvent, Ownership> {
    override suspend fun reduce(entity: Ownership, event: OwnershipEvent): Ownership {
        return if (entity.isLazyOwnership()) {
            val (value, lazyValue) = when (event) {
                is OwnershipEvent.TransferToEvent,
                is OwnershipEvent.ChangeLazyValueEvent -> {
                    (entity.value + event.value) to (entity.lazyValue + event.value)
                }
                is OwnershipEvent.TransferFromEvent -> (entity.value to entity.lazyValue)
                is OwnershipEvent.LazyTransferToEvent ->
                    throw IllegalArgumentException("This events can't be in this reducer")
            }
            entity.copy(value = value, lazyValue = lazyValue)
        } else {
            entity
        }
    }
}
