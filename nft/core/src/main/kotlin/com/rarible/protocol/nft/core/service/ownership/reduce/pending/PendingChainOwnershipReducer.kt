package com.rarible.protocol.nft.core.service.ownership.reduce.pending

import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipEvent
import com.rarible.protocol.nft.core.model.OwnershipId
import com.rarible.protocol.nft.core.service.EntityChainReducer
import com.rarible.protocol.nft.core.service.ownership.OwnershipPendingEventApplyPolicy
import org.springframework.stereotype.Component

@Component
class PendingChainOwnershipReducer(
    ownershipPendingEventApplyPolicy: OwnershipPendingEventApplyPolicy,
) : EntityChainReducer<OwnershipId, OwnershipEvent, Ownership>(
    ownershipPendingEventApplyPolicy
)
