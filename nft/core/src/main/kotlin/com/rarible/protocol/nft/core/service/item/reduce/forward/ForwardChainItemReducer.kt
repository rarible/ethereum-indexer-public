package com.rarible.protocol.nft.core.service.item.reduce.forward

import com.rarible.blockchain.scanner.ethereum.reduce.EntityChainReducer
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.service.item.ItemConfirmEventApplyPolicy
import org.springframework.stereotype.Component

@Component
class ForwardChainItemReducer(
    itemConfirmEventApplyPolicy: ItemConfirmEventApplyPolicy,
    forwardCreatorsItemReducer: ForwardCreatorsItemReducer,
    forwardValueItemReducer: ForwardValueItemReducer,
    forwardLazyValueItemReducer: ForwardLazyValueItemReducer,
    forwardOpenSeaLazyValueItemReducer: ForwardOpenSeaLazyValueItemReducer
) : EntityChainReducer<ItemId, ItemEvent, Item>(
    itemConfirmEventApplyPolicy,
    forwardCreatorsItemReducer,
    forwardLazyValueItemReducer,
    forwardOpenSeaLazyValueItemReducer,
    forwardValueItemReducer
)
