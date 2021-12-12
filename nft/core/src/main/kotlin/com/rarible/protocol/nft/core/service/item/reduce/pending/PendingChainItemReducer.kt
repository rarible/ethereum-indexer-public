package com.rarible.protocol.nft.core.service.item.reduce.pending

import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.service.EntityChainReducer
import com.rarible.protocol.nft.core.service.item.ItemPendingEventApplyPolicy
import org.springframework.stereotype.Component

@Component
class PendingChainItemReducer(
    itemPendingEventApplyPolicy: ItemPendingEventApplyPolicy,
    pendingCreatorsItemReducer: PendingCreatorsItemReducer
) : EntityChainReducer<ItemId, ItemEvent, Item>(
    itemPendingEventApplyPolicy,
    pendingCreatorsItemReducer
)

