package com.rarible.protocol.nft.core.service.item.reduce.forward

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent
import org.springframework.stereotype.Component

/**
 * This reducer is called on real mint of lazy item
 * It must monitor consistency of ownership value of item
 */
@Component
class ForwardLazyOwnershipValueItemReducer : Reducer<ItemEvent, Item> {
    override suspend fun reduce(entity: Item, event: ItemEvent): Item {
        return when (event) {
            is ItemEvent.ItemMintEvent -> {
                if (entity.isLazyItem()) {
                    val ownerships = entity.ownerships.toMutableMap()
                    val ownerValue = ownerships[event.owner] ?: EthUInt256.ZERO
                    if (event.supply > EthUInt256.ZERO && ownerValue > event.supply) {
                        ownerships[event.owner] = ownerValue - event.supply
                    } else {
                        ownerships.remove(event.owner)
                    }
                    entity.copy(ownerships = ownerships)
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