package com.rarible.protocol.order.core.service

import com.rarible.core.common.toOptional
import com.rarible.core.logging.LoggingUtils
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.log.LogEventsListener
import com.rarible.protocol.order.core.model.OrderExchangeHistory
import kotlinx.coroutines.reactor.mono
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class BlockProcessor(
    private val orderUpdateService: OrderUpdateService
) : LogEventsListener {

    override fun postProcessLogs(logs: List<LogEvent>): Mono<Void> {
        val orderHashes = logs.map { (it.data as OrderExchangeHistory).hash }.distinct()
        val run = mono {
            orderUpdateService.saveOrRemoveOnChainOrderVersions(logs)
            for (orderHash in orderHashes) {
                orderUpdateService.update(orderHash)
            }
        }
        return LoggingUtils.withMarker { marker ->
            run
                .toOptional()
                .elapsed()
                .doOnNext { logger.info(marker, "Process time: ${it.t1}ms") }
                .then()
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(BlockProcessor::class.java)
    }
}
