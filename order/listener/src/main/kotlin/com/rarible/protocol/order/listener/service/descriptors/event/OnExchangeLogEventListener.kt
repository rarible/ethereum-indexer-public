package com.rarible.protocol.order.listener.service.descriptors.event

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.ethereum.listener.log.OnLogEventListener
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.dto.OrderActivityDto
import com.rarible.protocol.order.core.converters.dto.OrderActivityConverter
import com.rarible.protocol.order.core.model.ItemType
import com.rarible.protocol.order.core.model.OnChainOrder
import com.rarible.protocol.order.core.model.OrderActivityResult
import com.rarible.protocol.order.core.model.OrderCancel
import com.rarible.protocol.order.core.model.OrderSide
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.core.producer.ProtocolOrderPublisher
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
@CaptureSpan(type = SpanType.EVENT)
class OnExchangeLogEventListener(
    private val eventPublisher: ProtocolOrderPublisher,
    private val orderActivityConverter: OrderActivityConverter
) : OnLogEventListener {

    private val logger = LoggerFactory.getLogger(javaClass)

    override val topics = ItemType.values().flatMap { it.topic }

    override fun onLogEvent(logEvent: LogEvent): Mono<Void> = mono {
        val dataType = logEvent.data::class.java.simpleName
        logger.info("Order log event: id=${logEvent.id}, dataType=$dataType")
        convert(logEvent, false)?.let { eventPublisher.publish(it) }
    }.then()

    override fun onRevertedLogEvent(logEvent: LogEvent): Mono<Void> = mono {
        val dataType = logEvent.data::class.java.simpleName
        logger.info("Order reverted log event: id=${logEvent.id}, dataType=$dataType")
        convert(logEvent, true)?.let { eventPublisher.publish(it) }
    }.then()

    private suspend fun convert(source: LogEvent, reverted: Boolean): OrderActivityDto? {
        if (source.status != LogEventStatus.CONFIRMED) {
            return null
        }
        val result = if (
            (source.data as? OrderSideMatch)?.side == OrderSide.LEFT
            || source.data is OnChainOrder
            || source.data is OrderCancel
        ) {
            OrderActivityResult.History(source)
        } else {
            null
        }
        return result?.let { orderActivityConverter.convert(it, reverted) }
    }
}
