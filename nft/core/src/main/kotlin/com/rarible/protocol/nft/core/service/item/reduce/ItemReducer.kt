package com.rarible.protocol.nft.core.service.item.reduce

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent
import com.rarible.protocol.nft.core.service.item.reduce.lazy.LazyItemReducer
import com.rarible.protocol.nft.core.service.item.reduce.status.EventStatusItemReducer
import com.rarible.protocol.nft.core.service.item.reduce.status.ItemDeleteReducer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ItemReducer(
    eventStatusItemReducer: EventStatusItemReducer,
    lazyItemReducer: LazyItemReducer
) : Reducer<ItemEvent, Item> {
    private val eventStatusItemReducer = ItemDeleteReducer.wrap(eventStatusItemReducer)
    private val lazyItemReducer = ItemDeleteReducer.wrap(lazyItemReducer)

    override suspend fun reduce(entity: Item, event: ItemEvent): Item {
        logEvent(entity, event)
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

    private fun logEvent(entity: Item, event: ItemEvent) {
        val log = event.log
        logger.info(
            "Item: {}, event: {}, status: {}, block: {}, logEvent: {}, minorLogIndex: {}",
            entity.id, event::class.java.simpleName, log.status, log.blockNumber, log.logIndex, log.minorLogIndex
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ItemReducer::class.java)
    }
}
