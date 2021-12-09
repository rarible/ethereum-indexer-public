package com.rarible.protocol.nft.core.service.item.reduce

import com.rarible.core.entity.reducer.service.ReversedReducer
import com.rarible.core.entity.reducer.service.RevertableEntityReversedReducer
import com.rarible.protocol.nft.core.model.BlockchainEntityEvent
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent
import org.springframework.stereotype.Component

@Component
class CompositeItemReducer(
    private val itemReducer: ItemReducer,
    reversedItemReducer: ReversedItemReducer
) : ReversedReducer<ItemEvent, Item> {
    private val reversedItemReducer = RevertableEntityReversedReducer(reversedItemReducer)

    override suspend fun reduce(entity: Item, event: ItemEvent): Item {
        return when (event.status) {
            BlockchainEntityEvent.Status.CONFIRMED,
            BlockchainEntityEvent.Status.PENDING -> {
                itemReducer.reduce(entity, event)
            }
            BlockchainEntityEvent.Status.REVERTED -> {
                reversedItemReducer.reduce(entity, event)
            }
        }
    }
}
