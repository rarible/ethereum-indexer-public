package com.rarible.protocol.order.core.service.block

import com.rarible.core.common.toOptional
import com.rarible.core.logging.LoggingUtils
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.log.LogEventsListener
import com.rarible.protocol.order.core.model.OrderExchangeHistory
import com.rarible.protocol.order.core.service.OrderUpdateService
import kotlinx.coroutines.reactor.mono
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class OrderBlockProcessor(
    private val orderUpdateService: OrderUpdateService
) : LogEventsListener {

    override fun postProcessLogs(logs: List<LogEvent>): Mono<Void> {
        val hashed = logs
            .map { log -> log.data }
            .filterIsInstance<OrderExchangeHistory>()
            .map { orderHistory -> orderHistory.hash }.distinct()

        val run = mono {
            orderUpdateService.saveOrRemoveOnChainOrderVersions(logs)
            for (hash in hashed) {
                orderUpdateService.update(hash)
            }
        }
        return LoggingUtils.withMarker { marker ->
            run
                .toOptional()
                .elapsed()
                .doOnNext { logger.info(marker, "Order logs process time: ${it.t1}ms") }
                .then()
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(OrderBlockProcessor::class.java)
    }
}

