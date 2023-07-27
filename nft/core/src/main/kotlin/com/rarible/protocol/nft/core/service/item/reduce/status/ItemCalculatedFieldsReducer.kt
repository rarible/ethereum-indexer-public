package com.rarible.protocol.nft.core.service.item.reduce.status

import com.rarible.blockchain.scanner.ethereum.model.EthereumBlockStatus
import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent

/**
 * This reducer make a final decision about item delete status
 * We mark item was deleted if it supply and lazySupply is zero
 * Also it calculates updatedAt filed
 */
class ItemCalculatedFieldsReducer : Reducer<ItemEvent, Item> {
    override suspend fun reduce(entity: Item, event: ItemEvent): Item {
        val deleted = entity.supply == EthUInt256.ZERO && entity.lazySupply == EthUInt256.ZERO
        val value = if (deleted) EthUInt256.ZERO else entity.supply

        val updatedAt =
            // We try to get timestamp of the latest blockchain event
            entity.revertableEvents.lastOrNull { it.log.status == EthereumBlockStatus.CONFIRMED }?.log?.createdAt
            ?: entity.date

        return entity.copy(deleted = deleted, supply = value, date = updatedAt)
    }
}
