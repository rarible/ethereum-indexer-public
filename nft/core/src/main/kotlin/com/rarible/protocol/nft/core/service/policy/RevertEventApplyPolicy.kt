package com.rarible.protocol.nft.core.service.policy

import com.rarible.blockchain.scanner.framework.model.Log
import com.rarible.core.entity.reducer.service.EventApplyPolicy
import com.rarible.protocol.nft.core.model.EthereumEntityEvent

open class RevertEventApplyPolicy<T : EthereumEntityEvent<T>> : EventApplyPolicy<T> {
    override fun reduce(events: List<T>, event: T): List<T> {
        val confirmedEvent = findConfirmedEvent(events, event)
        return if (confirmedEvent != null) events - event else events
    }

    override fun wasApplied(events: List<T>, event: T): Boolean {
        return findConfirmedEvent(events, event) != null
    }

    private fun findConfirmedEvent(events: List<T>, event: T): T? {
        return events.firstOrNull { current ->
            current.log.status == Log.Status.CONFIRMED && current.compareTo(event) == 0
        }
    }
}
