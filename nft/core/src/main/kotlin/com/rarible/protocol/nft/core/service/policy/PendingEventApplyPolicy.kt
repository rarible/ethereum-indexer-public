package com.rarible.protocol.nft.core.service.policy

import com.rarible.core.entity.reducer.service.EventApplyPolicy
import com.rarible.protocol.nft.core.model.BlockchainEntityEvent

open class PendingEventApplyPolicy<T : BlockchainEntityEvent<T>> : EventApplyPolicy<T> {

    override fun reduce(events: List<T>, event: T): List<T> {
        return events + event
    }

    override fun wasApplied(events: List<T>, event: T): Boolean {
        return findPendingEvent(events, event) != null
    }

    private fun findPendingEvent(events: List<T>, event: T): T? {
        return events.firstOrNull { current ->
            current.isPending && current.compareTo(event) == 0
        }
    }
}

