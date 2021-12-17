package com.rarible.protocol.nft.core.service.item.reduce.status

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent

/**
 * This reducer make a final decision about item delete status
 * We mark item was deleted if it supply and lazySupply is zero and also it has no pending logs
 */
class ItemDeleteReducer : Reducer<ItemEvent, Item> {
    override suspend fun reduce(entity: Item, event: ItemEvent): Item {
        val deleted = entity.supply == EthUInt256.ZERO && entity.lazySupply == EthUInt256.ZERO && entity.getPendingEvents().isEmpty()
        val value = if (deleted) EthUInt256.ZERO else entity.supply
        return entity.copy(deleted = deleted, supply = value)
    }
}
