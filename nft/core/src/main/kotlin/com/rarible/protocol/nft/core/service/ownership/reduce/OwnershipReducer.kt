package com.rarible.protocol.nft.core.service.ownership.reduce

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.protocol.nft.core.misc.combineIntoChain
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipEvent
import com.rarible.protocol.nft.core.service.LoggingReducer
import com.rarible.protocol.nft.core.service.ownership.reduce.lazy.LazyOwnershipReducer
import com.rarible.protocol.nft.core.service.ownership.reduce.status.EventStatusOwnershipReducer
import com.rarible.protocol.nft.core.service.ownership.reduce.status.OwnershipDeleteReducer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.springframework.stereotype.Component

@Component
@ExperimentalCoroutinesApi
class OwnershipReducer(
    eventStatusOwnershipReducer: EventStatusOwnershipReducer,
    lazyOwnershipReducer: LazyOwnershipReducer
) : Reducer<OwnershipEvent, Ownership> {

    private val eventStatusOwnershipReducer = combineIntoChain(
        LoggingReducer(),
        eventStatusOwnershipReducer,
        OwnershipDeleteReducer()
    )
    private val lazyOwnershipReducer = combineIntoChain(
        LoggingReducer(),
        lazyOwnershipReducer,
        OwnershipDeleteReducer()
    )

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
