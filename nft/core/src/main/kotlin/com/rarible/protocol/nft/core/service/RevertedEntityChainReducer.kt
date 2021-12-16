package com.rarible.protocol.nft.core.service

import com.rarible.core.entity.reducer.chain.ReducersChain
import com.rarible.core.entity.reducer.model.Entity
import com.rarible.core.entity.reducer.service.EventApplyPolicy
import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.core.entity.reducer.service.RevertedEntityReducer

abstract class RevertedEntityChainReducer<Id, Event, E : Entity<Id, Event, E>>(
    eventApplyPolicy: EventApplyPolicy<Event>,
    vararg reducers: Reducer<Event, E>
) : Reducer<Event, E> {

    private val reducer = RevertedEntityReducer(
        eventApplyPolicy = eventApplyPolicy,
        reversedReducer = ReducersChain(reducers.asList())
    )

    override suspend fun reduce(entity: E, event: Event): E {
        return reducer.reduce(entity, event)
    }
}
