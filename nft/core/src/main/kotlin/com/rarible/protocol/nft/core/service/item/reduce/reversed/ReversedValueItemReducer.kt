package com.rarible.protocol.nft.core.service.item.reduce.reversed

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent
import org.springframework.stereotype.Component

@Component
class ReversedValueItemReducer : Reducer<ItemEvent, Item> {
    override suspend fun reduce(entity: Item, event: ItemEvent): Item {
        val supply = when (event) {
            is ItemEvent.ItemMintEvent -> entity.supply - event.supply
            is ItemEvent.ItemBurnEvent -> entity.supply + event.supply
            is ItemEvent.ItemCreatorsEvent,
            is ItemEvent.ItemTransferEvent -> entity.supply

            is ItemEvent.LazyItemBurnEvent, is ItemEvent.LazyItemMintEvent ->
                throw IllegalArgumentException("This events can't be in this reducer")
        }
        return entity.copy(supply = supply, deleted = supply == EthUInt256.ZERO)
    }
}

