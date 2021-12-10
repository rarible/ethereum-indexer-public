package com.rarible.protocol.nft.core.service.ownership.reduce.reversed

import com.rarible.core.entity.reducer.service.ReversedReducer
import com.rarible.core.entity.reducer.service.RevertableEntityReversedReducer
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipEvent
import com.rarible.protocol.nft.core.service.ReversedReducersChain
import org.springframework.stereotype.Component

@Component
class ReversedChainOwnershipReducer(
    reversedOwnershipValueReducer: ReversedOwnershipValueReducer
) : ReversedReducer<OwnershipEvent, Ownership> {

    private val reducer = RevertableEntityReversedReducer(
        reversedReducer = ReversedReducersChain(listOf(reversedOwnershipValueReducer))
    )

    override suspend fun reduce(entity: Ownership, event: OwnershipEvent): Ownership {
        return reducer.reduce(entity, event)
    }
}
