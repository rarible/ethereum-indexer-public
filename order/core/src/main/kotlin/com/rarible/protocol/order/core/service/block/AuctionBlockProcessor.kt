package com.rarible.protocol.order.core.service.block

import com.rarible.core.common.toOptional
import com.rarible.core.logging.LoggingUtils
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.log.LogEventsListener
import com.rarible.protocol.order.core.model.AuctionHistory
import com.rarible.protocol.order.core.service.auction.AuctionUpdateService
import kotlinx.coroutines.reactor.mono
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class AuctionBlockProcessor(
    private val auctionUpdateService: AuctionUpdateService
) : LogEventsListener {

    val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun postProcessLogs(logs: List<LogEvent>): Mono<Void> {
        val hashes = logs
            .map { log -> log.data }
            .filterIsInstance<AuctionHistory>()
            .map { history -> history.hash }
            .distinct()

        val run = mono {
            for (hash in hashes) {
                auctionUpdateService.update(hash)
            }
        }
        return LoggingUtils.withMarker { marker ->
            run
                .toOptional()
                .elapsed()
                .doOnNext { logger.info(marker, "Auction logs process time: ${it.t1}ms") }
                .then()
        }
    }
}
