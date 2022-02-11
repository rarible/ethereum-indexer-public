package com.rarible.protocol.nft.core.service.ownership.reduce.inactive

import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipEvent
import com.rarible.protocol.nft.core.model.OwnershipId
import com.rarible.protocol.nft.core.service.RevertedEntityChainReducer
import com.rarible.protocol.nft.core.service.ownership.OwnershipInactiveEventApplyPolicy
import org.springframework.stereotype.Component

@Component
class InactiveChainOwnershipReducer(
    ownershipInactiveEventApplyPolicy: OwnershipInactiveEventApplyPolicy,
) : RevertedEntityChainReducer<OwnershipId, OwnershipEvent, Ownership>(
    ownershipInactiveEventApplyPolicy
)
