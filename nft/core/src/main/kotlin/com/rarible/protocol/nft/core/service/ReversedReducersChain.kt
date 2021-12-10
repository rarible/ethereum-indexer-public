package com.rarible.protocol.nft.core.service

import com.rarible.core.entity.reducer.service.ReversedReducer

open class ReversedReducersChain<Event, Entity>(
    private val reducers: List<ReversedReducer<Event, Entity>>
) : ReversedReducer<Event, Entity> {

    override suspend fun reduce(entity: Entity, event: Event): Entity {
        return reducers.fold(entity) { state, reducer ->
            return reducer.reduce(state, event)
        }
    }
}
