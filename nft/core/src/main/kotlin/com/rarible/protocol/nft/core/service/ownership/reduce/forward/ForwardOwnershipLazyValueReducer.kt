package com.rarible.protocol.nft.core.service.ownership.reduce.forward

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipEvent
import org.springframework.stereotype.Component

@Component
class ForwardOwnershipLazyValueReducer : Reducer<OwnershipEvent, Ownership> {
    override suspend fun reduce(entity: Ownership, event: OwnershipEvent): Ownership {
        // Handle event only if this ownership has 'lazyValue'
        // i.e. ownership was created on LazyMint event
        return if (entity.lazyValue > EthUInt256.ZERO) {
            val (value, lazyValue) = when (event) {
                // We receive this event on lazy item mint by other minter
                // and we need to decrease 'lazyValue' of this ownership
                // also need to decrease 'value' as it includes lazy part
                is OwnershipEvent.ChangeLazyValueEvent -> {
                    (entity.value - event.value) to (entity.lazyValue - event.value)
                }
                is OwnershipEvent.TransferToEvent -> {
                    // It is a mint event from owner of the item (very rare case)
                    // so wee need to do the same as for 'ChangeLazyValueEvent'
                    if (event.isMint()) {
                        (entity.value - event.value) to (entity.lazyValue - event.value)
                    } else {
                        (entity.value to entity.lazyValue)
                    }
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
