package com.rarible.protocol.nft.core.service

import com.rarible.core.entity.reducer.service.Reducer

abstract class ReducersChain<Event, Entity> : Reducer<Event, Entity> {
    abstract fun reducers(): List<Reducer<Event, Entity>>

    override suspend fun reduce(entity: Entity, event: Event): Entity {
        return reducers().fold(entity) { state, reducer ->
            return reducer.reduce(state, event)
        }
    }
}
