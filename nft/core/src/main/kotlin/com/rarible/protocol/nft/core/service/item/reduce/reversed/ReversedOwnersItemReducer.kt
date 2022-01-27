package com.rarible.protocol.nft.core.service.item.reduce.reversed

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent
import com.rarible.protocol.nft.core.service.item.reduce.ReduceContext
import com.rarible.protocol.nft.core.service.item.reduce.forward.ForwardOwnersItemReducer
import org.springframework.stereotype.Component
import kotlin.coroutines.coroutineContext

@Component
class ReversedOwnersItemReducer : Reducer<ItemEvent, Item> {
    private val delegate = ForwardOwnersItemReducer()

    override suspend fun reduce(entity: Item, event: ItemEvent): Item {
        val ctx = coroutineContext[ReduceContext]
        return if (ctx != null && ctx.skipOwnerships) {
            entity
        } else {
            when (event) {
                is ItemEvent.ItemTransferEvent,
                is ItemEvent.ItemMintEvent,
                is ItemEvent.ItemBurnEvent,
                is ItemEvent.ItemCreatorsEvent, -> delegate.reduce(entity, event.invert())
                is ItemEvent.LazyItemBurnEvent, is ItemEvent.LazyItemMintEvent ->
                    throw IllegalArgumentException("This events can't be in this reducer")
            }
        }
    }
}
