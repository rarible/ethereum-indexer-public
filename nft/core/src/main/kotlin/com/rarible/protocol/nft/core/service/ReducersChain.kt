package com.rarible.protocol.nft.core.service

import com.rarible.core.entity.reducer.service.Reducer

open class ReducersChain<Event, Entity>(
    private val reducers: List<Reducer<Event, Entity>>
) : Reducer<Event, Entity> {

    override suspend fun reduce(entity: Entity, event: Event): Entity {
        return reducers.fold(entity) { state, reducer ->
            reducer.reduce(state, event)
        }
    }
}

