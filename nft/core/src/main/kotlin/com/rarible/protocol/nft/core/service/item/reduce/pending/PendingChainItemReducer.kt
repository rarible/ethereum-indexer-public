package com.rarible.protocol.nft.core.service.item.reduce.pending

import com.rarible.protocol.nft.core.model.*
import com.rarible.protocol.nft.core.service.EntityChainReducer
import com.rarible.protocol.nft.core.service.ItemPendingEventApplyPolicy
import org.springframework.stereotype.Component

@Component
class PendingChainItemReducer(
    itemPendingEventApplyPolicy: ItemPendingEventApplyPolicy,
    pendingCreatorsItemReducer: PendingCreatorsItemReducer
) : EntityChainReducer<ItemId, ItemEvent, Item>(
    itemPendingEventApplyPolicy,
    pendingCreatorsItemReducer
)
