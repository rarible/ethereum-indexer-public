package com.rarible.protocol.nft.core.service.item.reduce.pending

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class PendingOwnersItemReducer : Reducer<ItemEvent, Item> {

    override suspend fun reduce(entity: Item, event: ItemEvent): Item {
        val ownerships = entity.ownerships.toMutableMap()
        return when (event) {
            is ItemEvent.ItemTransferEvent-> {
                val to = event.to
                entity.copy(ownerships = addOwner(ownerships, to))
            }
            is ItemEvent.ItemMintEvent -> {
                val owner = event.owner
                entity.copy(ownerships = addOwner(ownerships, owner))
            }
            is ItemEvent.ItemBurnEvent,
            is ItemEvent.ItemCreatorsEvent -> entity

            is ItemEvent.LazyItemBurnEvent, is ItemEvent.LazyItemMintEvent ->
                throw IllegalArgumentException("This events can't be in this reducer")
        }
    }

    private fun addOwner(ownerships: MutableMap<Address, EthUInt256>, owner: Address): MutableMap<Address, EthUInt256> {
        val ownerValue = ownerships[owner] ?: EthUInt256.ZERO
        ownerships[owner] = ownerValue
        return ownerships
    }
}
