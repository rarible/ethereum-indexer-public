package com.rarible.protocol.nft.core.service.item.reduce.inactive

import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.service.EntityChainReducer
import com.rarible.protocol.nft.core.service.item.ItemInactiveEventApplyPolicy
import com.rarible.protocol.nft.core.service.item.reduce.reversed.ReversedLazyValueItemReducer
import com.rarible.protocol.nft.core.service.item.reduce.reversed.ReversedOwnersItemReducer
import com.rarible.protocol.nft.core.service.item.reduce.reversed.ReversedValueItemReducer
import org.springframework.stereotype.Component

@Component
class InactiveChainItemReducer(
    itemPendingEventApplyPolicy: ItemInactiveEventApplyPolicy,
    reversedValueItemReducer: ReversedValueItemReducer,
    reversedLazyValueItemReducer: ReversedLazyValueItemReducer,
    reversedOwnersItemReducer: ReversedOwnersItemReducer
) : EntityChainReducer<ItemId, ItemEvent, Item>(
    itemPendingEventApplyPolicy,
    reversedValueItemReducer,
    reversedLazyValueItemReducer,
    reversedOwnersItemReducer
)
