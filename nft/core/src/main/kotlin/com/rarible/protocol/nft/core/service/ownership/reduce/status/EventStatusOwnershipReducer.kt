package com.rarible.protocol.nft.core.service.ownership.reduce.status

import com.rarible.blockchain.scanner.ethereum.reduce.EventStatusReducer
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipEvent
import com.rarible.protocol.nft.core.model.OwnershipId
import com.rarible.protocol.nft.core.service.ownership.reduce.forward.ForwardChainOwnershipReducer
import com.rarible.protocol.nft.core.service.ownership.reduce.reversed.ReversedChainOwnershipReducer
import com.rarible.protocol.nft.core.service.ownership.reduce.reversed.RevertOwnershipCompactEventsReducer
import org.springframework.stereotype.Component

@Component
class EventStatusOwnershipReducer(
    forwardOwnershipReducer: ForwardChainOwnershipReducer,
    reversedOwnershipReducer: ReversedChainOwnershipReducer,
    revertOwnershipCompactEventsReducer: RevertOwnershipCompactEventsReducer,
) : EventStatusReducer<OwnershipId, OwnershipEvent, Ownership>(
    forwardOwnershipReducer,
    reversedOwnershipReducer,
    revertOwnershipCompactEventsReducer,
)
