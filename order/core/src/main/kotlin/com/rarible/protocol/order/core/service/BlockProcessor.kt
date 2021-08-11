package com.rarible.protocol.order.core.service

import com.rarible.core.common.toOptional
import com.rarible.core.logging.LoggingUtils
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.log.LogEventsListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class BlockProcessor(
    private val orderReduceService: OrderReduceService
) : LogEventsListener {

    override fun postProcessLogs(logs: List<LogEvent>): Mono<Void> {
        return LoggingUtils.withMarker { marker ->
            Mono.`when`(orderReduceService.onExchange(logs))
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
