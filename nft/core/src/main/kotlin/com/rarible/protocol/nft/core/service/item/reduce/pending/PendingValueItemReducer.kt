package com.rarible.protocol.nft.core.service.item.reduce.pending

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent
import org.springframework.stereotype.Component

@Component
class PendingValueItemReducer : Reducer<ItemEvent, Item> {
    override suspend fun reduce(entity: Item, event: ItemEvent): Item {
        return when (event) {
            is ItemEvent.ItemMintEvent -> {
                entity.copy(deleted = event.supply == EthUInt256.ZERO)
            }
            is ItemEvent.ItemBurnEvent,
            is ItemEvent.ItemCreatorsEvent,
            is ItemEvent.ItemTransferEvent-> entity
            is ItemEvent.LazyItemBurnEvent, is ItemEvent.LazyItemMintEvent ->
                throw IllegalArgumentException("This events can't be in this reducer")
        }
    }
}

