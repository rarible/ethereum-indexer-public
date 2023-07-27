package com.rarible.protocol.order.core.service.block.auction

import com.rarible.blockchain.scanner.ethereum.model.EthereumBlockStatus
import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.blockchain.scanner.framework.listener.LogRecordEventSubscriber
import com.rarible.core.common.EventTimeMarks
import com.rarible.core.common.nowMillis
import com.rarible.protocol.order.core.converters.dto.AuctionActivityConverter
import com.rarible.protocol.order.core.misc.addIndexerIn
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

    // TODO looks fishy
    override suspend fun onLogRecordEvents(events: List<LogRecordEvent>) {
        val indexerInMark = nowMillis()
        val auctionEvents = events
            .filter { log -> (log.record.asEthereumLogRecord()).data is AuctionHistory }

        val reduceEvent = auctionEvents
            .map { log -> Pair(log, AuctionReduceEvent(log.record.asEthereumLogRecord())) }
            .distinctBy { it.second }

        // Reduce events first in order to have Auctions in DB
        auctionReduceService.onEvents(reduceEvent.map { it.second })
        // Then send all kafka Auction Activity events with attached actual Auctions
        auctionEvents
            .forEach {
                val record = it.record.asEthereumLogRecord()
                if (record.status == EthereumBlockStatus.CONFIRMED) {
                    publicActivity(record, it.eventTimeMarks.addIndexerIn(indexerInMark))
                }
            }
    }

    suspend fun publicActivity(logEvent: ReversedEthereumLogRecord, eventTimeMarks: EventTimeMarks) {
        logger.info("Auction log event: id=${logEvent.id}, dataType=${logEvent.data::class.java.simpleName}")
        auctionActivityConverter.convert(logEvent)?.let { eventPublisher.publish(it, eventTimeMarks) }
    }
}
