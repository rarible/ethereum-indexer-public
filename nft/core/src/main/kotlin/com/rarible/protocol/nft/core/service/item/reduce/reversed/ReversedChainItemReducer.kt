package com.rarible.protocol.nft.core.service.item.reduce.reversed

import com.rarible.blockchain.scanner.ethereum.reduce.RevertedEntityChainReducer
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.service.item.ItemRevertEventApplyPolicy
import org.springframework.stereotype.Component

@Component
class ReversedChainItemReducer(
    itemRevertEventApplyPolicy: ItemRevertEventApplyPolicy,
    reversedValueItemReducer: ReversedValueItemReducer,
    reversedLazyValueItemReducer: ReversedLazyValueItemReducer,
    reversedOpenSeaLazyValueItemReducer: ReversedOpenSeaLazyValueItemReducer
) : RevertedEntityChainReducer<ItemId, ItemEvent, Item>(
    itemRevertEventApplyPolicy,
    reversedValueItemReducer,
    reversedLazyValueItemReducer,
    reversedOpenSeaLazyValueItemReducer
)
