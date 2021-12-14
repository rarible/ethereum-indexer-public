package com.rarible.protocol.nft.core.service.item.reduce.status

import com.rarible.core.entity.reducer.chain.ReducersChain
import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent

class ItemDeleteReducer : Reducer<ItemEvent, Item> {
    override suspend fun reduce(entity: Item, event: ItemEvent): Item {
        val deleted = entity.supply == EthUInt256.ZERO && entity.lazySupply == EthUInt256.ZERO && entity.getPendingEvents().isEmpty()
        val value = if (deleted) EthUInt256.ZERO else entity.supply
        return entity.copy(deleted = deleted, supply = value)
    }

    companion object {
        fun wrap(reducer: Reducer<ItemEvent, Item>): Reducer<ItemEvent, Item> {
            return ReducersChain(listOf(reducer, ItemDeleteReducer()))
        }
    }
}
