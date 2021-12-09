package com.rarible.protocol.nft.core.service.item.reduce

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent
import org.springframework.stereotype.Component
import java.lang.IllegalStateException

@Component
class LazyItemReducer : Reducer<ItemEvent, Item> {
    override suspend fun reduce(entity: Item, event: ItemEvent): Item {
        return if (entity.lastLazyEventTimestamp >= event.timestamp) {
            entity
        } else {
            val lazySupply = when (event) {
                is ItemEvent.LazyItemMintEvent -> entity.lazySupply + event.supply
                is ItemEvent.LazyItemBurnEvent -> EthUInt256.ZERO
                is ItemEvent.ItemBurnEvent, is ItemEvent.ItemMintEvent -> {
                    throw IllegalStateException("This events can't be in this reducer")
                }
            }
            entity.copy(
                lazySupply = lazySupply,
                lastLazyEventTimestamp = event.timestamp,
                deleted = lazySupply == EthUInt256.ZERO
            )
        }
    }
}
