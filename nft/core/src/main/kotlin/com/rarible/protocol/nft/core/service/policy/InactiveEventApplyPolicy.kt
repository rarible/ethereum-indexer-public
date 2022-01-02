package com.rarible.protocol.nft.core.service.policy

import com.rarible.protocol.nft.core.model.EthereumEntityEvent

open class InactiveEventApplyPolicy<T : EthereumEntityEvent<T>> : PendingEventApplyPolicy<T>() {

    override fun reduce(events: List<T>, event: T): List<T> {
        val pendingEvent = findPendingEvent(events, event)
        return if (pendingEvent != null) events - pendingEvent else events
    }
}
