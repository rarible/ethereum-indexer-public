package com.rarible.protocol.nft.core.service.item.reduce.forward

import com.rarible.protocol.nft.core.model.*
import com.rarible.protocol.nft.core.service.EntityChainReducer
import com.rarible.protocol.nft.core.service.ItemConfirmEventApplyPolicy
import org.springframework.stereotype.Component

@Component
class ForwardChainItemReducer(
    itemConfirmEventApplyPolicy: ItemConfirmEventApplyPolicy,
    forwardCreatorsItemReducer: ForwardCreatorsItemReducer,
    forwardLazyValueItemReducer: ForwardLazyValueItemReducer,
    forwardValueItemReducer: ForwardValueItemReducer,
    forwardOwnersItemReducer: ForwardOwnersItemReducer
) : EntityChainReducer<ItemId, ItemEvent, Item>(
    itemConfirmEventApplyPolicy,
    forwardCreatorsItemReducer,
    forwardLazyValueItemReducer,
    forwardValueItemReducer,
    forwardOwnersItemReducer
)
