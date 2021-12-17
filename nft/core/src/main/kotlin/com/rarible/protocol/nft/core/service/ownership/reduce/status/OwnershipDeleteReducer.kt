package com.rarible.protocol.nft.core.service.ownership.reduce.status

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipEvent

class OwnershipDeleteReducer : Reducer<OwnershipEvent, Ownership> {
    override suspend fun reduce(entity: Ownership, event: OwnershipEvent): Ownership {
        val deleted = entity.lazyValue == EthUInt256.ZERO && entity.value == EthUInt256.ZERO && entity.getPendingEvents().isEmpty()
        val value = if (deleted) EthUInt256.ZERO else entity.value
        return entity.copy(deleted = deleted, value = value)
    }
}
