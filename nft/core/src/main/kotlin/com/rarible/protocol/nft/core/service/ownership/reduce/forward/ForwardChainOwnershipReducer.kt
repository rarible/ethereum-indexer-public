package com.rarible.protocol.nft.core.service.ownership.reduce.forward

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.core.entity.reducer.service.RevertableEntityReducer
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipEvent
import com.rarible.protocol.nft.core.service.ConfirmEventRevertService
import com.rarible.protocol.nft.core.service.ReducersChain
import org.springframework.stereotype.Component

@Component
class ForwardChainOwnershipReducer(
    forwardOwnershipValueReducer: ForwardOwnershipValueReducer,
    confirmEventRevertService: ConfirmEventRevertService<OwnershipEvent>
) : Reducer<OwnershipEvent, Ownership> {

    private val reducer = RevertableEntityReducer(
        eventRevertService = confirmEventRevertService,
        reducer = ReducersChain(
            listOf(
                forwardOwnershipValueReducer
            )
        )
    )

    override suspend fun reduce(entity: Ownership, event: OwnershipEvent): Ownership {
        return reducer.reduce(entity, event)
    }
}
