package com.rarible.protocol.order.core.service.block

import com.rarible.core.common.toOptional
import com.rarible.core.logging.LoggingUtils
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.ethereum.log.LogEventsListener
import com.rarible.protocol.order.core.converters.dto.AuctionActivityConverter
import com.rarible.protocol.order.core.model.AuctionHistory
import com.rarible.protocol.order.core.model.AuctionReduceEvent
import com.rarible.protocol.order.core.producer.ProtocolAuctionPublisher
import com.rarible.protocol.order.core.service.auction.AuctionReduceService
import kotlinx.coroutines.reactor.mono
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class AuctionBlockProcessor(
    private val auctionReduceService: AuctionReduceService,
    private val eventPublisher: ProtocolAuctionPublisher,
    private val auctionActivityConverter: AuctionActivityConverter
) : LogEventsListener {

    val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun postProcessLogs(logs: List<LogEvent>): Mono<Void> {
        val auctionEvents = logs
            .filter { log -> log.data is AuctionHistory }

        val events = auctionEvents.map { log -> AuctionReduceEvent(log) }
            .distinct()

        return LoggingUtils.withMarker { marker ->
            mono {
                // Reduce events first in order to have Auctions in DB
                try {
                    auctionReduceService.onEvents(events)
                } catch (e: Exception) {
                    logger.error("OOPS", e)
                }
                // Then send all kafka Auction Activity events with attached actual Auctions
                auctionEvents.filter { it.status == LogEventStatus.CONFIRMED }
                    .forEach { publicActivity(it) }
            }.toOptional()
                .elapsed()
                .doOnNext { logger.info(marker, "Auction logs process time: ${it.t1}ms") }
                .then()
        }
    }

    suspend fun publicActivity(logEvent: LogEvent) {
        logger.info("Auction log event: id=${logEvent.id}, dataType=${logEvent.data::class.java.simpleName}")
        auctionActivityConverter.convert(logEvent)?.let { eventPublisher.publish(it) }
    }
}
