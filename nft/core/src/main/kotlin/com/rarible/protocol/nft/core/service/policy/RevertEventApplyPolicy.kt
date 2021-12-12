package com.rarible.protocol.nft.core.service.policy

import com.rarible.core.entity.reducer.service.EventApplyPolicy
import com.rarible.protocol.nft.core.model.BlockchainEntityEvent

open class RevertEventApplyPolicy<T : BlockchainEntityEvent<T>> : EventApplyPolicy<T> {
    override fun reduce(events: List<T>, event: T): List<T> {
        val confirmedEvent = findConfirmedEvent(events, event)
        return if (confirmedEvent != null) events - event else events
    }

    override fun wasApplied(events: List<T>, event: T): Boolean {
        return findConfirmedEvent(events, event) != null
    }

    private fun findConfirmedEvent(events: List<T>, event: T): T? {
        return events.firstOrNull { current ->
            current.isConfirmed && current.compareTo(event) == 0
        }
    }
}

