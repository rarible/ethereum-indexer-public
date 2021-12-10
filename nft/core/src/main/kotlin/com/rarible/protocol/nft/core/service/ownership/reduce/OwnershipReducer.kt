package com.rarible.protocol.nft.core.service.ownership.reduce

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipEvent
import com.rarible.protocol.nft.core.service.ownership.reduce.status.EventStatusOwnershipReducer
import org.springframework.stereotype.Component

@Component
class OwnershipReducer(
    private val eventStatusOwnershipReducer: EventStatusOwnershipReducer
) : Reducer<OwnershipEvent, Ownership> {

    override suspend fun reduce(entity: Ownership, event: OwnershipEvent): Ownership {
        return when (event) {
            is OwnershipEvent.TransferFromEvent,
            is OwnershipEvent.TransferToEvent -> {
                eventStatusOwnershipReducer.reduce(entity, event)
            }
        }
    }
}
