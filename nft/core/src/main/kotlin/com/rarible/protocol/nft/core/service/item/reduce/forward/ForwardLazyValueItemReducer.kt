package com.rarible.protocol.nft.core.service.item.reduce.forward

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent
import org.springframework.stereotype.Component

/**
 * This reducer is called on real mint of lazy item
 * It must monitor consistency of lazyValue of item
 */
@Component
class ForwardLazyValueItemReducer : Reducer<ItemEvent, Item> {
    override suspend fun reduce(entity: Item, event: ItemEvent): Item {
        return when (event) {
            is ItemEvent.ItemMintEvent -> {
                if (entity.isLazyItem()) {
                    //On mint of lazy item it's lazySupply decreases
                    val lazySupply = entity.lazySupply - event.supply
                    //We also should to decreases supply as it is contains lazy part
                    val supply = entity.supply - event.supply
                    entity.copy(lazySupply = lazySupply, supply = supply)
                } else {
                    entity
                }
            }
            is ItemEvent.ItemTransferEvent,
            is ItemEvent.ItemBurnEvent,
            is ItemEvent.ItemCreatorsEvent -> entity
            is ItemEvent.LazyItemBurnEvent, is ItemEvent.LazyItemMintEvent ->
                throw IllegalArgumentException("This events can't be in this reducer")
        }
    }
}
