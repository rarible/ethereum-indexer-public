package com.rarible.protocol.nft.core.service.policy

import com.rarible.core.entity.reducer.service.EventApplyPolicy
import com.rarible.protocol.nft.core.model.EthereumEntityEvent

open class PendingEventApplyPolicy<T : EthereumEntityEvent<T>> : EventApplyPolicy<T> {
    override fun reduce(events: List<T>, event: T): List<T> {
        return events + event
    }

    override fun wasApplied(events: List<T>, event: T): Boolean {
        return findEvent(events, event) != null
    }

    private fun findEvent(events: List<T>, event: T): T? {
        // Need find CONFIRMED or PENDING logs
        // we are comparing CONFIRMED log with key-comparator
        return events.firstOrNull { current -> event.compareTo(current) == 0 }
    }
}
