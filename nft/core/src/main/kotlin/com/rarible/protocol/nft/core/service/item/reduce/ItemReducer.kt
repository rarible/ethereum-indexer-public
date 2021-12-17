package com.rarible.protocol.nft.core.service.item.reduce

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.protocol.nft.core.misc.combineIntoChain
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent
import com.rarible.protocol.nft.core.service.LoggingReducer
import com.rarible.protocol.nft.core.service.item.reduce.lazy.LazyItemReducer
import com.rarible.protocol.nft.core.service.item.reduce.status.EventStatusItemReducer
import com.rarible.protocol.nft.core.service.item.reduce.status.ItemDeleteReducer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.springframework.stereotype.Component

@Component
@ExperimentalCoroutinesApi
class ItemReducer(
    eventStatusItemReducer: EventStatusItemReducer,
    lazyItemReducer: LazyItemReducer
) : Reducer<ItemEvent, Item> {

    private val eventStatusItemReducer = combineIntoChain(
        LoggingReducer(),
        eventStatusItemReducer,
        ItemDeleteReducer()
    )
    private val lazyItemReducer = combineIntoChain(
        LoggingReducer(),
        lazyItemReducer,
        ItemDeleteReducer()
    )

    override suspend fun reduce(entity: Item, event: ItemEvent): Item {
        return when (event) {
            is ItemEvent.ItemBurnEvent,
            is ItemEvent.ItemMintEvent,
            is ItemEvent.ItemCreatorsEvent,
            is ItemEvent.ItemTransferEvent -> {
                eventStatusItemReducer.reduce(entity, event)
            }
            is ItemEvent.LazyItemBurnEvent, is ItemEvent.LazyItemMintEvent -> {
                lazyItemReducer.reduce(entity, event)
            }
        }
    }
}
