package com.rarible.protocol.order.core.service.block.auction

import com.rarible.blockchain.scanner.ethereum.model.EthereumLogStatus
import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.blockchain.scanner.framework.listener.LogRecordEventSubscriber
import com.rarible.protocol.order.core.converters.dto.AuctionActivityConverter
import com.rarible.protocol.order.core.misc.asEthereumLogRecord
import com.rarible.protocol.order.core.model.AuctionHistory
import com.rarible.protocol.order.core.model.AuctionReduceEvent
import com.rarible.protocol.order.core.producer.ProtocolAuctionPublisher
import com.rarible.protocol.order.core.service.auction.AuctionReduceService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
@Qualifier("auction-event-subscriber")
class AuctionEventSubscriber(
    private val auctionReduceService: AuctionReduceService,
    private val eventPublisher: ProtocolAuctionPublisher,
    private val auctionActivityConverter: AuctionActivityConverter
) : LogRecordEventSubscriber {

    val logger: Logger = LoggerFactory.getLogger(javaClass)

    override suspend fun onLogRecordEvents(events: List<LogRecordEvent>) {
        val auctionEvents = events
            .map { event -> event.record.asEthereumLogRecord() }
            .filter { log -> log.data is AuctionHistory }

        val reduceEvent = auctionEvents
            .map { log -> AuctionReduceEvent(log) }
            .distinct()

        // Reduce events first in order to have Auctions in DB
        auctionReduceService.onEvents(reduceEvent)
        // Then send all kafka Auction Activity events with attached actual Auctions
        auctionEvents
            .filter { it.status == EthereumLogStatus.CONFIRMED }
            .forEach { publicActivity(it) }
    }

    suspend fun publicActivity(logEvent: ReversedEthereumLogRecord) {
        logger.info("Auction log event: id=${logEvent.id}, dataType=${logEvent.data::class.java.simpleName}")
        auctionActivityConverter.convert(logEvent)?.let { eventPublisher.publish(it) }
    }
}

