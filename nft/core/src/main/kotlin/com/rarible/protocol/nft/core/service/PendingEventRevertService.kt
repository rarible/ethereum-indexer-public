package com.rarible.protocol.nft.core.service

import com.rarible.core.entity.reducer.service.EventRevertService
import com.rarible.protocol.nft.core.model.BlockchainEntityEvent

open class PendingEventRevertService<T : BlockchainEntityEvent<T>>() : EventRevertService<T> {
    override fun canBeReverted(last: T, current: T): Boolean {
        require(last.status == BlockchainEntityEvent.Status.PENDING)

        if (current.status == BlockchainEntityEvent.Status.PENDING) {
            return true
        }
    }
}

