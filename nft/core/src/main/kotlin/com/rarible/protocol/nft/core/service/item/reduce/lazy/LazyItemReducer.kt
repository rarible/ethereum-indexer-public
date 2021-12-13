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
                    val lazySupply = entity.lazySupply + event.supply
                    entity.copy(
                        lazySupply = lazySupply,
                        supply = lazySupply,
                        creators = event.creators,
                        ownerships = event.creators.firstOrNull()?.let { mapOf(it.account to lazySupply) } ?: emptyMap(),
                        creatorsFinal = true,
                        lastLazyEventTimestamp = event.timestamp,
                        deleted = lazySupply == EthUInt256.ZERO
                    )
                }
                is ItemEvent.LazyItemBurnEvent -> {
                    entity.copy(
                        lazySupply = EthUInt256.ZERO,
                        lastLazyEventTimestamp = event.timestamp,
                        deleted = true
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
