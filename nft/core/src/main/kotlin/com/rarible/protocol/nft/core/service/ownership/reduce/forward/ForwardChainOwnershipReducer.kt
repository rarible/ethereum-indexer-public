package com.rarible.protocol.nft.core.service.ownership.reduce.forward

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.core.entity.reducer.service.RevertableEntityReducer
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipEvent
import com.rarible.protocol.nft.core.service.EntityEventRevertService
import com.rarible.protocol.nft.core.service.ReducersChain
import org.springframework.stereotype.Component

@Component
class ForwardChainOwnershipReducer(
    forwardOwnershipValueReducer: ForwardOwnershipValueReducer,
    forwardCreatorsOwnershipReducer: ForwardCreatorsOwnershipReducer,
    entityEventRevertService: EntityEventRevertService<OwnershipEvent>
) : ReducersChain<OwnershipEvent, Ownership>() {

    private val reducers = listOf(
        RevertableEntityReducer(entityEventRevertService, forwardOwnershipValueReducer),
        forwardCreatorsOwnershipReducer
    )

    override fun reducers(): List<Reducer<OwnershipEvent, Ownership>> {
        return reducers
    }
}
