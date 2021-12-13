package com.rarible.protocol.nft.core.service.item.reduce.pending

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent
import org.springframework.stereotype.Component

@Component
class PendingOwnersItemReducer : Reducer<ItemEvent, Item> {

    override suspend fun reduce(entity: Item, event: ItemEvent): Item {
        return when (event) {
            is ItemEvent.ItemTransferEvent-> {
                val ownerships = entity.ownerships.toMutableMap()
                val toValue = ownerships[event.to] ?: EthUInt256.ZERO
                val fromValue = ownerships[event.from] ?: EthUInt256.ZERO

                ownerships[event.to] = toValue
                ownerships[event.from] = fromValue
                entity.copy(ownerships = ownerships)
            }
            is ItemEvent.ItemMintEvent -> {
                val ownerships = entity.ownerships.toMutableMap()
                val ownerValue = ownerships[event.owner] ?: EthUInt256.ZERO

                ownerships[event.owner] = ownerValue
                entity.copy(ownerships = ownerships)
            }
            is ItemEvent.ItemBurnEvent,
            is ItemEvent.ItemCreatorsEvent -> entity

            is ItemEvent.LazyItemBurnEvent, is ItemEvent.LazyItemMintEvent ->
                throw IllegalArgumentException("This events can't be in this reducer")
        }
    }
}
