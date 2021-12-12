package com.rarible.protocol.nft.core.service.item.reduce.status

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.protocol.nft.core.model.BlockchainEntityEvent
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent
import com.rarible.protocol.nft.core.service.item.reduce.forward.ForwardChainItemReducer
import com.rarible.protocol.nft.core.service.item.reduce.pending.PendingChainItemReducer
import com.rarible.protocol.nft.core.service.item.reduce.reversed.ReversedChainItemReducer
import org.springframework.stereotype.Component

@Component
class EventStatusItemReducer(
    private val forwardChainItemReducer: ForwardChainItemReducer,
    private val reversedChainItemReducer: ReversedChainItemReducer,
    private val pendingChainItemReducer: PendingChainItemReducer
) : Reducer<ItemEvent, Item> {

    override suspend fun reduce(entity: Item, event: ItemEvent): Item {
        return when (event.status) {
            BlockchainEntityEvent.Status.CONFIRMED -> {
                forwardChainItemReducer.reduce(entity, event)
            }
            BlockchainEntityEvent.Status.PENDING -> {
                pendingChainItemReducer.reduce(entity, event)
            }
            BlockchainEntityEvent.Status.REVERTED -> {
                reversedChainItemReducer.reduce(entity, event)
            }
            BlockchainEntityEvent.Status.INACTIVE -> TODO()
        }
    }
}
