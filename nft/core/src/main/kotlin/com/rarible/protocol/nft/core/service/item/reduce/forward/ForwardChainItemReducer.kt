package com.rarible.protocol.nft.core.service.item.reduce.forward

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.core.entity.reducer.service.RevertableEntityReducer
import com.rarible.protocol.nft.core.model.*
import com.rarible.protocol.nft.core.service.ConfirmEventRevertService
import com.rarible.protocol.nft.core.service.ReducersChain
import org.springframework.stereotype.Component

@Component
class ForwardChainItemReducer(
    forwardCreatorsItemReducer: ForwardCreatorsItemReducer,
    forwardLazyValueItemReducer: ForwardLazyValueItemReducer,
    forwardValueItemReducer: ForwardValueItemReducer,
    forwardOwnersItemReducer: ForwardOwnersItemReducer,
    confirmEventRevertService: ConfirmEventRevertService<ItemEvent>
) : Reducer<ItemEvent, Item> {

    private val reducer = RevertableEntityReducer(
        eventRevertService = confirmEventRevertService,
        reducer = ReducersChain(
            listOf(
                forwardCreatorsItemReducer,
                forwardValueItemReducer,
                forwardOwnersItemReducer,
                forwardLazyValueItemReducer
            )
        )
    )

    override suspend fun reduce(entity: Item, event: ItemEvent): Item {
        return reducer.reduce(entity, event)
    }
}
