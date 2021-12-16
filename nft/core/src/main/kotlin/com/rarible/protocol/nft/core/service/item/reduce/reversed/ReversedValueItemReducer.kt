package com.rarible.protocol.nft.core.service.item.reduce.reversed

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent
import com.rarible.protocol.nft.core.service.item.reduce.forward.ForwardValueItemReducer
import org.springframework.stereotype.Component

@Component
class ReversedValueItemReducer : Reducer<ItemEvent, Item> {
    private val forwardValueItemReducer = ForwardValueItemReducer()

    override suspend fun reduce(entity: Item, event: ItemEvent): Item {
        return when (event) {
            is ItemEvent.ItemMintEvent,
            is ItemEvent.ItemBurnEvent,
            is ItemEvent.ItemCreatorsEvent,
            is ItemEvent.ItemTransferEvent -> forwardValueItemReducer.reduce(entity, event.invert())
            is ItemEvent.LazyItemBurnEvent, is ItemEvent.LazyItemMintEvent ->
                throw IllegalArgumentException("This events can't be in this reducer")
        }
    }
}

