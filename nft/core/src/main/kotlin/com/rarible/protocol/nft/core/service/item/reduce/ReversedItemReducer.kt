package com.rarible.protocol.nft.core.service.item.reduce

import com.rarible.core.entity.reducer.service.ReversedReducer
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent
import org.springframework.stereotype.Component

@Component
class ReversedItemReducer : ReversedReducer<ItemEvent, Item> {
    override suspend fun reduce(entity: Item, event: ItemEvent): Item {
        val supply = when (event) {
            is ItemEvent.ItemMintEvent -> entity.supply - event.supply
            is ItemEvent.ItemBurnEvent -> entity.supply + event.supply
        }
        return entity.copy(supply = supply, deleted = supply == EthUInt256.ZERO)
    }
}
