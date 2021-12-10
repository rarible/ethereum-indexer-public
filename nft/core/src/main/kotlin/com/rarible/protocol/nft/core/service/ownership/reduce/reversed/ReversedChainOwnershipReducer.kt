package com.rarible.protocol.nft.core.service.ownership.reduce.reversed

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.core.entity.reducer.service.RevertableEntityReversedReducer
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipEvent
import com.rarible.protocol.nft.core.service.ReducersChain
import org.springframework.stereotype.Component

@Component
class ReversedChainOwnershipReducer(
    reversedOwnershipValueReducer: ReversedOwnershipValueReducer,
) : ReducersChain<OwnershipEvent, Ownership>() {

    private val reducers = listOf(
        RevertableEntityReversedReducer(reversedOwnershipValueReducer)
    )

    override fun reducers(): List<Reducer<OwnershipEvent, Ownership>> {
        return reducers
    }
}
