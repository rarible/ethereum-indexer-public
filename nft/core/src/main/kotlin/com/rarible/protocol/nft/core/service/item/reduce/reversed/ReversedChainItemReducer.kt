package com.rarible.protocol.nft.core.service.item.reduce.reversed

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.core.entity.reducer.service.RevertableEntityReversedReducer
import com.rarible.protocol.nft.core.model.*
import com.rarible.protocol.nft.core.service.ReversedReducersChain
import org.springframework.stereotype.Component

@Component
class ReversedChainItemReducer(
    reversedValueItemReducer: ReversedValueItemReducer,
    reversedLazyValueItemReducer: ReversedLazyValueItemReducer,
) : Reducer<ItemEvent, Item> {

    private val chain = RevertableEntityReversedReducer(
        ReversedReducersChain(listOf(reversedValueItemReducer, reversedLazyValueItemReducer))
    )

    override suspend fun reduce(entity: Item, event: ItemEvent): Item {
        return chain.reduce(entity, event)
    }
}
