package com.rarible.protocol.nft.core.service.item.reduce

import com.rarible.blockchain.scanner.ethereum.reduce.LoggingReducer
import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.protocol.nft.core.misc.combineIntoChain
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent
import com.rarible.protocol.nft.core.service.item.reduce.lazy.LazyItemReducer
import com.rarible.protocol.nft.core.service.item.reduce.status.CompactItemEventsReducer
import com.rarible.protocol.nft.core.service.item.reduce.status.EventStatusItemReducer
import com.rarible.protocol.nft.core.service.item.reduce.status.ItemCalculatedFieldsReducer
import org.springframework.stereotype.Component

@Component
class ItemReducer(
    eventStatusItemReducer: EventStatusItemReducer,
    compactItemEventsReducer: CompactItemEventsReducer,
    lazyItemReducer: LazyItemReducer,
    itemMetricReducer: ItemMetricReducer
) : Reducer<ItemEvent, Item> {

    private val eventStatusItemReducer = combineIntoChain(
        LoggingReducer(),
        itemMetricReducer,
        eventStatusItemReducer,
        compactItemEventsReducer,
        ItemCalculatedFieldsReducer()
    )
    private val lazyItemReducer = combineIntoChain(
        LoggingReducer(),
        itemMetricReducer,
        lazyItemReducer,
        ItemCalculatedFieldsReducer()
    )

    override suspend fun reduce(entity: Item, event: ItemEvent): Item {
        return when (event) {
            is ItemEvent.ItemBurnEvent,
            is ItemEvent.ItemMintEvent,
            is ItemEvent.OpenSeaLazyItemMintEvent,
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
