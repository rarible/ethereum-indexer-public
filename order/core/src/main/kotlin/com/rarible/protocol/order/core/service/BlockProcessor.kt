package com.rarible.protocol.order.core.service

import com.rarible.core.common.toOptional
import com.rarible.core.logging.LoggingUtils
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.log.LogEventsListener
import com.rarible.protocol.order.core.model.OrderExchangeHistory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux

@Service
class BlockProcessor(
    private val orderReduceService: OrderReduceService
) : LogEventsListener {

    override fun postProcessLogs(logs: List<LogEvent>): Mono<Void> {
        val orderHashes = logs.map { (it.data as OrderExchangeHistory).hash }.distinct()
        return LoggingUtils.withMarker { marker ->
            orderHashes.toFlux()
                .concatMap { orderReduceService.update(orderHash = it) }
                .then()
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
