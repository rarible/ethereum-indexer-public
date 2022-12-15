package com.rarible.protocol.order.core.service.block.pool

import com.rarible.blockchain.scanner.ethereum.reduce.EntityEventsSubscriber
import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.protocol.order.core.service.block.handler.PoolEthereumEventHandler
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
@Qualifier("pool-event-subscriber")
class PoolEventSubscriber(
    private val poolEthereumEventHandler: PoolEthereumEventHandler
) : EntityEventsSubscriber {
    override suspend fun onEntityEvents(events: List<LogRecordEvent>) {
        poolEthereumEventHandler.handle(events)
    }
}