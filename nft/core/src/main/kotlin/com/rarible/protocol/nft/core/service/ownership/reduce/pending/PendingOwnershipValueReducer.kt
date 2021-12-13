package com.rarible.protocol.nft.core.service.ownership.reduce.pending

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipEvent
import org.springframework.stereotype.Component

@Component
class PendingOwnershipValueReducer : Reducer<OwnershipEvent, Ownership> {
    override suspend fun reduce(entity: Ownership, event: OwnershipEvent): Ownership {
        return when (event) {
            is OwnershipEvent.TransferToEvent -> entity.copy(deleted = entity.value + event.value == EthUInt256.ZERO)
            is OwnershipEvent.TransferFromEvent -> entity
            is OwnershipEvent.LazyTransferToEvent ->
                throw IllegalArgumentException("This events can't be in this reducer")
        }
    }
}

