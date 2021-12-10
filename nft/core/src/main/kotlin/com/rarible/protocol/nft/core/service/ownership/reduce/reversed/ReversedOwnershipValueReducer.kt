package com.rarible.protocol.nft.core.service.ownership.reduce.reversed

import com.rarible.core.entity.reducer.service.ReversedReducer
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipEvent
import org.springframework.stereotype.Component

@Component
class ReversedOwnershipValueReducer : ReversedReducer<OwnershipEvent, Ownership> {
    override suspend fun reduce(entity: Ownership, event: OwnershipEvent): Ownership {
        val value = when (event) {
            is OwnershipEvent.TransferToEvent -> entity.value - event.value
            is OwnershipEvent.TransferFromEvent -> entity.value + event.value
        }
        return entity.copy(value = value, deleted = value == EthUInt256.ZERO)
    }
}
