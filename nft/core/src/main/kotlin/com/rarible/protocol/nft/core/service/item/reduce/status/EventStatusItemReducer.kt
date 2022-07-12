package com.rarible.protocol.nft.core.service.item.reduce.status

import com.rarible.blockchain.scanner.ethereum.model.EthereumLogStatus
import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent
import com.rarible.protocol.nft.core.service.item.reduce.forward.ForwardChainItemReducer
import com.rarible.protocol.nft.core.service.item.reduce.reversed.ReversedChainItemReducer
import org.springframework.stereotype.Component

@Component
class EventStatusItemReducer(
    private val forwardChainItemReducer: ForwardChainItemReducer,
    private val reversedChainItemReducer: ReversedChainItemReducer
) : Reducer<ItemEvent, Item> {

    override suspend fun reduce(entity: Item, event: ItemEvent): Item {
        return when (event.log.status) {
            EthereumLogStatus.CONFIRMED -> forwardChainItemReducer.reduce(entity, event)
            EthereumLogStatus.REVERTED -> reversedChainItemReducer.reduce(entity, event)
            EthereumLogStatus.PENDING,
            EthereumLogStatus.INACTIVE,
            EthereumLogStatus.DROPPED -> entity
        }
    }
}
