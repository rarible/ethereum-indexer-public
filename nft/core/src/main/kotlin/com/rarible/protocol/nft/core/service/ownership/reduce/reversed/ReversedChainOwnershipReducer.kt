package com.rarible.protocol.nft.core.service.ownership.reduce.reversed

import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipEvent
import com.rarible.protocol.nft.core.model.OwnershipId
import com.rarible.protocol.nft.core.service.OwnershipRevertEventApplyPolicy
import com.rarible.protocol.nft.core.service.RevertedEntityChainReducer
import org.springframework.stereotype.Component

@Component
class ReversedChainOwnershipReducer(
    eventApplyPolicy: OwnershipRevertEventApplyPolicy,
    reversedOwnershipValueReducer: ReversedOwnershipValueReducer
) : RevertedEntityChainReducer<OwnershipId, OwnershipEvent, Ownership>(
    eventApplyPolicy,
    reversedOwnershipValueReducer
)
