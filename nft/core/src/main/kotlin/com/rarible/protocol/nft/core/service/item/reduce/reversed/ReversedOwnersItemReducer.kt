package com.rarible.protocol.nft.core.service.item.reduce.reversed

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent
import org.springframework.stereotype.Component

@Component
class ReversedOwnersItemReducer : Reducer<ItemEvent, Item> {

    override suspend fun reduce(entity: Item, event: ItemEvent): Item {
        return when (event) {
            is ItemEvent.ItemTransferEvent-> {
                val ownerships = entity.ownerships.toMutableMap()
                val toValue = ownerships[event.to] ?: EthUInt256.ZERO
                val fromValue = ownerships[event.from] ?: EthUInt256.ZERO

                if (event.value != EthUInt256.ZERO && toValue > event.value) {
                    ownerships[event.to] = toValue - event.value
                }
                if (event.value != EthUInt256.ZERO) {
                    ownerships[event.from] = fromValue + event.value
                }
                entity.copy(ownerships = ownerships)
            }
            is ItemEvent.ItemMintEvent,
            is ItemEvent.ItemBurnEvent,
            is ItemEvent.ItemCreatorsEvent -> entity

            is ItemEvent.LazyItemBurnEvent, is ItemEvent.LazyItemMintEvent ->
                throw IllegalArgumentException("This events can't be in this reducer")
        }
    }
}
