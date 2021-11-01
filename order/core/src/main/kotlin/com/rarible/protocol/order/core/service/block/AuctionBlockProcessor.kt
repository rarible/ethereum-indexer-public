package com.rarible.protocol.order.core.service.block

import com.rarible.core.common.toOptional
import com.rarible.core.logging.LoggingUtils
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.log.LogEventsListener
import com.rarible.protocol.order.core.model.AuctionHistory
import com.rarible.protocol.order.core.model.AuctionReduceEvent
import com.rarible.protocol.order.core.service.auction.AuctionReduceService
import kotlinx.coroutines.reactor.mono
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class AuctionBlockProcessor(
    private val auctionReduceService: AuctionReduceService
) : LogEventsListener {

    val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun postProcessLogs(logs: List<LogEvent>): Mono<Void> {
        val events = logs
            .filter { log -> log.data is AuctionHistory }
            .map { log -> AuctionReduceEvent(log) }
            .distinct()

        return LoggingUtils.withMarker { marker ->
            mono { auctionReduceService.onEvents(events) }
                .toOptional()
                .elapsed()
                .doOnNext { logger.info(marker, "Auction logs process time: ${it.t1}ms") }
                .then()
        }
    }
}
