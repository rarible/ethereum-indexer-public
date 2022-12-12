package com.rarible.protocol.order.core.service.block

import com.rarible.core.common.toOptional
import com.rarible.core.logging.LoggingUtils
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.log.LogEventsListener
import com.rarible.protocol.order.core.service.block.handler.OrderEthereumEventHandler
import com.rarible.protocol.order.core.service.block.handler.PoolEthereumEventHandler
import kotlinx.coroutines.reactor.mono
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class OrderBlockProcessor(
    private val orderEthereumEventHandler: OrderEthereumEventHandler,
    private val poolEthereumEventHandler: PoolEthereumEventHandler
) : LogEventsListener {

    override fun postProcessLogs(logs: List<LogEvent>): Mono<Void> {
        val blockNumber = logs.firstOrNull()?.blockNumber
        logger.info("Order logs process start, blockNumber=$blockNumber")

        return LoggingUtils.withMarker { marker ->
            mono {
                orderEthereumEventHandler.handle(logs)
                poolEthereumEventHandler.handle(logs)
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

