package com.rarible.protocol.order.listener.service.descriptors.event

import com.rarible.ethereum.listener.log.OnLogEventListener
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.dto.AuctionActivityDto
import com.rarible.protocol.order.core.converters.dto.AuctionActivityConverter
import com.rarible.protocol.order.core.model.AuctionHistoryType
import com.rarible.protocol.order.core.producer.ProtocolAuctionPublisher
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono

//@Component
//@CaptureSpan(type = SpanType.EVENT)
class OnAuctionLogEventListener(
    private val eventPublisher: ProtocolAuctionPublisher,
    private val auctionActivityConverter: AuctionActivityConverter
) : OnLogEventListener {
    private val logger = LoggerFactory.getLogger(javaClass)

    override val topics = AuctionHistoryType.values().flatMap { it.topic }

    override fun onLogEvent(logEvent: LogEvent): Mono<Void> = mono {
        logger.info(
            "Auction log event: id=${logEvent.id}, dataType=${logEvent.data::class.java.simpleName}"
        )
        convert(logEvent)?.let { eventPublisher.publish(it) }
    }.then()

    private suspend fun convert(source: LogEvent): AuctionActivityDto? {
        if (source.status != LogEventStatus.CONFIRMED) {
            return null
        }
        return auctionActivityConverter.convert(source)
    }
}
