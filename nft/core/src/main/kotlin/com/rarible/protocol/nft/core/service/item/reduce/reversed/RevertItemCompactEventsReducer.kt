package com.rarible.protocol.nft.core.service.item.reduce.reversed

import com.rarible.blockchain.scanner.ethereum.reduce.RevertCompactEventsReducer
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent
import com.rarible.protocol.nft.core.model.ItemId
import org.springframework.stereotype.Component

@Component
class RevertItemCompactEventsReducer : RevertCompactEventsReducer<ItemId, ItemEvent, Item>() {
    override fun merge(reverted: ItemEvent, compact: ItemEvent): ItemEvent {
        return reverted.withSupply(compact.supply())
    }
}