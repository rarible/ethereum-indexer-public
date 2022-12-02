package com.rarible.protocol.order.core.service.block.order

import com.rarible.core.common.toOptional
import com.rarible.core.logging.LoggingUtils
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.log.LogEventsListener
import com.rarible.protocol.order.core.model.OrderHistory
import com.rarible.protocol.order.core.model.PoolHistory
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.core.service.pool.listener.PoolOrderEventListener
import kotlinx.coroutines.reactor.mono
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
@Deprecated("Should be removed after switch to the new scanner")
class OrderBlockProcessor(
    private val orderUpdateService: OrderUpdateService,
    private val poolOrderEventListener: PoolOrderEventListener
) : LogEventsListener {

    override fun postProcessLogs(logs: List<LogEvent>): Mono<Void> {
        val blockNumber = logs.firstOrNull()?.blockNumber
        logger.info("Order logs process start, blockNumber=$blockNumber")

        val hashes = logs
            .map { log -> log.data }
            .filterIsInstance<OrderHistory>()
            .map { orderHistory -> orderHistory.hash }
            .distinct()

        val poolEvents = logs
            .filter { log -> log.data is PoolHistory }

        return LoggingUtils.withMarker { marker ->
            mono {
                for (hash in hashes) {
                    orderUpdateService.update(hash)
                }
                for (poolEvent in poolEvents) {
                    poolOrderEventListener.onPoolEvent(poolEvent)
                }
            }
                .toOptional()
                .elapsed()
                .doOnNext { logger.info(marker, "Order logs process time (blockNumber=$blockNumber): ${it.t1}ms") }
                .then()
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(OrderBlockProcessor::class.java)
    }
}

