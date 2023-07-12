package com.rarible.protocol.nft.core.service.ownership.reduce.reversed

import com.rarible.blockchain.scanner.ethereum.reduce.RevertCompactEventsReducer
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipEvent
import com.rarible.protocol.nft.core.model.OwnershipId
import org.springframework.stereotype.Component

@Component
class RevertOwnershipCompactEventsReducer : RevertCompactEventsReducer<OwnershipId, OwnershipEvent, Ownership>() {

    override fun merge(reverted: OwnershipEvent, compact: OwnershipEvent): OwnershipEvent {
        return reverted.withValue(compact.value)
    }
}