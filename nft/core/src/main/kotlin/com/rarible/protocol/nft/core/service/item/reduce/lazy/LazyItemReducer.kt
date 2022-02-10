package com.rarible.protocol.nft.core.service.item.reduce.lazy

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent
import org.springframework.stereotype.Component

@Component
class LazyItemReducer : Reducer<ItemEvent, Item> {
    override suspend fun reduce(entity: Item, event: ItemEvent): Item {
        return if (entity.lastLazyEventTimestamp != null && entity.lastLazyEventTimestamp >= event.timestamp) {
            entity
        } else {
            return when (event) {
                is ItemEvent.LazyItemMintEvent -> {
                    entity.copy(
                        lazySupply = event.supply,
                        supply = event.supply,
                        creators = event.creators,
                        creatorsFinal = true,
                        lastLazyEventTimestamp = event.timestamp
                    )
                }
                is ItemEvent.LazyItemBurnEvent -> {
                    entity.copy(
                        supply = entity.supply - entity.lazySupply,
                        lazySupply = EthUInt256.ZERO,
                        lastLazyEventTimestamp = event.timestamp
                    )
                }
                is ItemEvent.ItemBurnEvent,
                is ItemEvent.ItemMintEvent,
                is ItemEvent.ItemCreatorsEvent,
                is ItemEvent.ItemTransferEvent -> {
                    throw IllegalArgumentException("This events can't be in this reducer")
                }
            }
        }
    }
}
