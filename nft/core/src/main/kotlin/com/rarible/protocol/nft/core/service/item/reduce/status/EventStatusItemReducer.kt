package com.rarible.protocol.nft.core.service.item.reduce.status

import com.rarible.blockchain.scanner.framework.model.Log
import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent
import com.rarible.protocol.nft.core.service.item.reduce.forward.ForwardChainItemReducer
import com.rarible.protocol.nft.core.service.item.reduce.inactive.InactiveChainItemReducer
import com.rarible.protocol.nft.core.service.item.reduce.pending.PendingChainItemReducer
import com.rarible.protocol.nft.core.service.item.reduce.reversed.ReversedChainItemReducer
import org.springframework.stereotype.Component

@Component
class EventStatusItemReducer(
    private val forwardChainItemReducer: ForwardChainItemReducer,
    private val reversedChainItemReducer: ReversedChainItemReducer,
    private val pendingChainItemReducer: PendingChainItemReducer,
    private val inactiveChainItemReducer: InactiveChainItemReducer
) : Reducer<ItemEvent, Item> {

    override suspend fun reduce(entity: Item, event: ItemEvent): Item {
        return when (event.log.status) {
            Log.Status.CONFIRMED -> forwardChainItemReducer.reduce(entity, event)
            Log.Status.PENDING -> pendingChainItemReducer.reduce(entity, event)
            Log.Status.REVERTED -> reversedChainItemReducer.reduce(entity, event)
            Log.Status.INACTIVE,
            Log.Status.DROPPED -> inactiveChainItemReducer.reduce(entity, event)
        }
    }
}
