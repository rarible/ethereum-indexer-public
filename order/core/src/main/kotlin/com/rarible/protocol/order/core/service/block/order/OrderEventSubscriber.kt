package com.rarible.protocol.order.core.service.block.order

import com.rarible.blockchain.scanner.ethereum.reduce.EntityEventsSubscriber
import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.protocol.order.core.misc.asEthereumLogRecord
import com.rarible.protocol.order.core.model.OrderExchangeHistory
import com.rarible.protocol.order.core.service.OrderUpdateService
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
@Qualifier("order-event-subscriber")
class OrderEventSubscriber(
    private val orderUpdateService: OrderUpdateService,
) : EntityEventsSubscriber {
    override suspend fun onEntityEvents(events: List<LogRecordEvent>) {
        val hashes = events
            .map { it.record.asEthereumLogRecord().data }
            .filterIsInstance<OrderExchangeHistory>()
            .map { history -> history.hash }
            .distinct()

        for (hash in hashes) {
            orderUpdateService.update(hash)
        }
    }
}

