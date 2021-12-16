package com.rarible.protocol.nft.core.service.ownership.reduce.reversed

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipEvent
import org.springframework.stereotype.Component

@Component
class RevertedOwnershipLazyValueReducer : Reducer<OwnershipEvent, Ownership> {
    override suspend fun reduce(entity: Ownership, event: OwnershipEvent): Ownership {
        // Handle event only if this ownership has 'lazyValue'
        // i.e. ownership was created on LazyMint event
        return if (entity.lazyValue > EthUInt256.ZERO) {
            val (value, lazyValue) = when (event) {
                // We receive this event on revert lazy item realt mint by other minter
                // and we need to increase 'lazyValue' of this ownership
                // also need to increase 'value' as it includes lazy part
                is OwnershipEvent.ChangeLazyValueEvent -> {
                    (entity.value + event.value) to (entity.lazyValue + event.value)
                }
                is OwnershipEvent.TransferToEvent -> {
                    // It is a revert mint event from owner of the item (very rare case)
                    // so wee need to do the same as for 'ChangeLazyValueEvent'
                    if (event.isMint()) {
                        (entity.value + event.value) to (entity.lazyValue + event.value)
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
