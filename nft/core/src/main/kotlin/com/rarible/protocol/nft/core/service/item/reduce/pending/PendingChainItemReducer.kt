package com.rarible.protocol.nft.core.service.item.reduce.pending

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.protocol.nft.core.model.*
import com.rarible.protocol.nft.core.service.ReducersChain
import org.springframework.stereotype.Component

@Component
class PendingChainItemReducer(
    pendingCreatorsItemReducer: PendingCreatorsItemReducer,
) : Reducer<ItemEvent, Item> {

    private val reducer = ReducersChain(
        listOf(
            pendingCreatorsItemReducer
        )
    )

    override suspend fun reduce(entity: Item, event: ItemEvent): Item {
        return reducer.reduce(entity, event)
    }
}
