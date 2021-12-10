package com.rarible.protocol.nft.core.service.item.reduce.forward

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.core.entity.reducer.service.RevertableEntityReducer
import com.rarible.protocol.nft.core.model.*
import com.rarible.protocol.nft.core.service.EntityEventRevertService
import com.rarible.protocol.nft.core.service.ReducersChain
import org.springframework.stereotype.Component

@Component
class ForwardChainItemReducer(
    forwardCreatorsItemReducer: ForwardCreatorsItemReducer,
    forwardLazyValueItemReducer: ForwardLazyValueItemReducer,
    forwardValueItemReducer: ForwardValueItemReducer,
    entityEventRevertService: EntityEventRevertService<ItemEvent>
) : Reducer<ItemEvent, Item> {

    private val reducer = RevertableEntityReducer(
        eventRevertService = entityEventRevertService,
        reducer = ReducersChain(listOf(forwardCreatorsItemReducer, forwardValueItemReducer, forwardLazyValueItemReducer))
    )

    override suspend fun reduce(entity: Item, event: ItemEvent): Item {
        return reducer.reduce(entity, event)
    }
}
