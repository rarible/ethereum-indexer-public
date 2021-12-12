package com.rarible.protocol.nft.core.service

import com.rarible.core.entity.reducer.chain.ReducersChain
import com.rarible.core.entity.reducer.model.Entity
import com.rarible.core.entity.reducer.service.EntityReducer
import com.rarible.core.entity.reducer.service.EventApplyPolicy
import com.rarible.core.entity.reducer.service.Reducer

abstract class EntityChainReducer<Id, Event, E : Entity<Id, Event, E>>(
    eventApplyPolicy: EventApplyPolicy<Event>,
    vararg reducers: Reducer<Event, E>
) : Reducer<Event, E> {

    private val reducer = EntityReducer(
        eventApplyPolicy = eventApplyPolicy,
        reducer = ReducersChain(reducers.asList())
    )

    override suspend fun reduce(entity: E, event: Event): E {
        return reducer.reduce(entity, event)
    }
}
