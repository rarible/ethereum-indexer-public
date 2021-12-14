package com.rarible.protocol.nft.core.service.ownership.reduce.forward

import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipEvent
import com.rarible.protocol.nft.core.model.OwnershipId
import com.rarible.protocol.nft.core.service.EntityChainReducer
import com.rarible.protocol.nft.core.service.ownership.OwnershipConfirmEventApplyPolicy
import org.springframework.stereotype.Component

@Component
class ForwardChainOwnershipReducer(
    eventApplyPolicy: OwnershipConfirmEventApplyPolicy,
    forwardOwnershipValueReducer: ForwardOwnershipValueReducer,
    forwardOwnershipLazyValueReducer: ForwardOwnershipLazyValueReducer
) : EntityChainReducer<OwnershipId, OwnershipEvent, Ownership>(
    eventApplyPolicy,
    forwardOwnershipValueReducer,
    forwardOwnershipLazyValueReducer
)
