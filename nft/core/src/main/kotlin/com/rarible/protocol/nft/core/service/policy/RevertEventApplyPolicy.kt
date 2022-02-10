package com.rarible.protocol.nft.core.service.policy

import com.rarible.blockchain.scanner.ethereum.model.EthereumLogStatus
import com.rarible.core.entity.reducer.service.EventApplyPolicy
import com.rarible.protocol.nft.core.model.EthereumEntityEvent

open class RevertEventApplyPolicy<T : EthereumEntityEvent<T>> : EventApplyPolicy<T> {
    override fun reduce(events: List<T>, event: T): List<T> {
        require(events.isNotEmpty()) {
            "Can't revert from empty list (event=$event)"
        }
        require(event >= events.first()) {
            "Can't revert to old event (events=$events, event=$event)"
        }
        val confirmedEvent = findConfirmedEvent(events, event)
        return if (confirmedEvent != null) {
            require(events.last() == confirmedEvent) {
                "Event must revert from tail of list. Revert event: $event, event list=$events"
            }
            events - confirmedEvent
        } else events
    }

    override fun wasApplied(events: List<T>, event: T): Boolean {
        return findConfirmedEvent(events, event) != null
    }

    private fun findConfirmedEvent(events: List<T>, event: T): T? {
        return events.firstOrNull { current ->
            current.log.status == EthereumLogStatus.CONFIRMED && current.compareTo(event) == 0
        }
    }
}
