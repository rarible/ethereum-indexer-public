package com.rarible.protocol.nft.core.service.ownership.reduce.status

import com.rarible.blockchain.scanner.ethereum.model.EthereumLogStatus
import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipEvent
import com.rarible.protocol.nft.core.service.ownership.reduce.forward.ForwardChainOwnershipReducer
import com.rarible.protocol.nft.core.service.ownership.reduce.inactive.InactiveChainOwnershipReducer
import com.rarible.protocol.nft.core.service.ownership.reduce.pending.PendingChainOwnershipReducer
import com.rarible.protocol.nft.core.service.ownership.reduce.reversed.ReversedChainOwnershipReducer
import org.springframework.stereotype.Component

@Component
class EventStatusOwnershipReducer(
    private val forwardOwnershipReducer: ForwardChainOwnershipReducer,
    private val pendingOwnershipReducer: PendingChainOwnershipReducer,
    private val reversedOwnershipReducer: ReversedChainOwnershipReducer,
    private val inactiveOwnershipReducer: InactiveChainOwnershipReducer
) : Reducer<OwnershipEvent, Ownership> {

    override suspend fun reduce(entity: Ownership, event: OwnershipEvent): Ownership {
        return when (event.log.status) {
            EthereumLogStatus.CONFIRMED -> forwardOwnershipReducer.reduce(entity, event)
            EthereumLogStatus.PENDING -> pendingOwnershipReducer.reduce(entity, event)
            EthereumLogStatus.REVERTED -> reversedOwnershipReducer.reduce(entity, event)
            EthereumLogStatus.INACTIVE,
            EthereumLogStatus.DROPPED -> inactiveOwnershipReducer.reduce(entity, event)
        }
    }
}
