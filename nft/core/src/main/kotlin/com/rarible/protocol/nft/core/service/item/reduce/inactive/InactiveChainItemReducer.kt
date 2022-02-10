package com.rarible.protocol.nft.core.service.item.reduce.inactive

import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.service.RevertedEntityChainReducer
import com.rarible.protocol.nft.core.service.item.ItemInactiveEventApplyPolicy
import org.springframework.stereotype.Component

@Component
class InactiveChainItemReducer(
    itemInactiveEventApplyPolicy: ItemInactiveEventApplyPolicy
) : RevertedEntityChainReducer<ItemId, ItemEvent, Item>(
    itemInactiveEventApplyPolicy
)