package com.rarible.protocol.nft.core.service.item.reduce.reversed

import com.rarible.protocol.nft.core.model.*
import com.rarible.protocol.nft.core.service.ItemRevertEventApplyPolicy
import com.rarible.protocol.nft.core.service.RevertedEntityChainReducer
import org.springframework.stereotype.Component

@Component
class ReversedChainItemReducer(
    itemRevertEventApplyPolicy: ItemRevertEventApplyPolicy,
    reversedValueItemReducer: ReversedValueItemReducer,
    reversedLazyValueItemReducer: ReversedLazyValueItemReducer,
    reversedOwnersItemReducer: ReversedOwnersItemReducer
) : RevertedEntityChainReducer<ItemId, ItemEvent, Item>(
    itemRevertEventApplyPolicy,
    reversedValueItemReducer,
    reversedLazyValueItemReducer,
    reversedOwnersItemReducer
)
