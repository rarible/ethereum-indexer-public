package com.rarible.protocol.order.core.service.block.pool

import com.rarible.blockchain.scanner.ethereum.reduce.EntityEventsSubscriber
import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.protocol.order.core.misc.asEthereumLogRecord
import com.rarible.protocol.order.core.model.PoolHistory
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.core.service.pool.listener.PoolEventListener
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
@Qualifier("pool-event-subscriber")
class PoolEventSubscriber(
    private val orderUpdateService: OrderUpdateService,
    private val poolEventListeners: List<PoolEventListener>
) : EntityEventsSubscriber {
    override suspend fun onEntityEvents(events: List<LogRecordEvent>) {
        val ethereumLogRecords = events
            .map { it.record.asEthereumLogRecord() }

        val hashes = ethereumLogRecords
            .map { it.data as PoolHistory }
            .map { history -> history.hash }
            .distinct()

        coroutineScope {
            hashes
                .map { hash -> async { orderUpdateService.update(hash) } }
                .awaitAll()
        }
        coroutineScope {
            ethereumLogRecords
                .map { event -> async {
                    poolEventListeners.forEach {
                        it.onPoolEvent(event)
                    } }
                }
                .awaitAll()
        }
    }
}

