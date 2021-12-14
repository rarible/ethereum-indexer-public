package com.rarible.protocol.nft.core.service.ownership.reduce.forward

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipEvent
import org.springframework.stereotype.Component

@Component
class ForwardOwnershipLazyValueReducer : Reducer<OwnershipEvent, Ownership> {
    override suspend fun reduce(entity: Ownership, event: OwnershipEvent): Ownership {
        return if (entity.lazyValue > EthUInt256.ZERO) {
            val (value, lazyValue) = when (event) {
                is OwnershipEvent.TransferToEvent,
                is OwnershipEvent.ChangeLazyValueEvent -> {
                    (entity.value - event.value) to (entity.lazyValue - event.value)
                }
                is OwnershipEvent.TransferFromEvent -> (entity.value to entity.lazyValue)
                is OwnershipEvent.LazyTransferToEvent ->
                    throw IllegalArgumentException("This events can't be in this reducer")
            }
            entity.copy(value = value, lazyValue = lazyValue)
        } else {
            entity
        }
    }
}
