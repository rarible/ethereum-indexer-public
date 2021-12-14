package com.rarible.protocol.nft.core.service.ownership.reduce

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipEvent
import com.rarible.protocol.nft.core.service.ownership.reduce.lazy.LazyOwnershipReducer
import com.rarible.protocol.nft.core.service.ownership.reduce.status.EventStatusOwnershipReducer
import com.rarible.protocol.nft.core.service.ownership.reduce.status.OwnershipDeleteReducer
import org.springframework.stereotype.Component

@Component
class OwnershipReducer(
    eventStatusOwnershipReducer: EventStatusOwnershipReducer,
    private val lazyOwnershipReducer: LazyOwnershipReducer
) : Reducer<OwnershipEvent, Ownership> {
    private val eventStatusOwnershipReducer = OwnershipDeleteReducer.wrap(eventStatusOwnershipReducer)

    override suspend fun reduce(entity: Ownership, event: OwnershipEvent): Ownership {
        return when (event) {
            is OwnershipEvent.TransferFromEvent,
            is OwnershipEvent.TransferToEvent,
            is OwnershipEvent.ChangeLazyValueEvent -> {
                eventStatusOwnershipReducer.reduce(entity, event)
            }
            is OwnershipEvent.LazyTransferToEvent -> {
                lazyOwnershipReducer.reduce(entity, event)
            }
        }
    }
}
