package com.rarible.protocol.order.core.service.block.order

import com.rarible.blockchain.scanner.ethereum.reduce.EntityEventsSubscriber
import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.protocol.order.core.service.block.handler.OrderEthereumEventHandler
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
@Qualifier("order-event-subscriber")
class OrderEventSubscriber(
    private val orderEthereumEventHandler: OrderEthereumEventHandler,
) : EntityEventsSubscriber {
    override suspend fun onEntityEvents(events: List<LogRecordEvent>) {
        orderEthereumEventHandler.handle(events)
    }
}

