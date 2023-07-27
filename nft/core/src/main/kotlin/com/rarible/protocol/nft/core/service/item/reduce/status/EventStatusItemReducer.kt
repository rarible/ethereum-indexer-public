package com.rarible.protocol.nft.core.service.item.reduce.status

import com.rarible.blockchain.scanner.ethereum.reduce.EventStatusReducer
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.service.item.reduce.forward.ForwardChainItemReducer
import com.rarible.protocol.nft.core.service.item.reduce.reversed.ReversedChainItemReducer
import com.rarible.protocol.nft.core.service.item.reduce.reversed.RevertItemCompactEventsReducer
import org.springframework.stereotype.Component

@Component
class EventStatusItemReducer(
    forwardChainItemReducer: ForwardChainItemReducer,
    reversedChainItemReducer: ReversedChainItemReducer,
    revertItemCompactEventsReducer: RevertItemCompactEventsReducer,
) : EventStatusReducer<ItemId, ItemEvent, Item>(
    forwardChainItemReducer,
    reversedChainItemReducer,
    revertItemCompactEventsReducer
)
