package com.rarible.protocol.nft.core.service.item.reduce.forward

import com.rarible.protocol.nft.core.model.*
import com.rarible.protocol.nft.core.service.EntityChainReducer
import com.rarible.protocol.nft.core.service.item.ItemConfirmEventApplyPolicy
import org.springframework.stereotype.Component

@Component
class ForwardChainItemReducer(
    itemConfirmEventApplyPolicy: ItemConfirmEventApplyPolicy,
    forwardCreatorsItemReducer: ForwardCreatorsItemReducer,
    forwardValueItemReducer: ForwardValueItemReducer,
    forwardLazyValueItemReducer: ForwardLazyValueItemReducer,
    forwardLazyOwnershipValueItemReducer: ForwardLazyOwnershipValueItemReducer,
    forwardOwnersItemReducer: ForwardOwnersItemReducer,
    forwardOpenSeaLazyValueItemReducer: ForwardOpenSeaLazyValueItemReducer
) : EntityChainReducer<ItemId, ItemEvent, Item>(
    itemConfirmEventApplyPolicy,
    forwardCreatorsItemReducer,
    forwardLazyValueItemReducer,
    forwardLazyOwnershipValueItemReducer,
    forwardValueItemReducer,
    forwardOwnersItemReducer,
    forwardOpenSeaLazyValueItemReducer
)