package com.rarible.protocol.order.listener.service.descriptors.event

import com.rarible.ethereum.listener.log.OnLogEventListener
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.dto.OrderActivityDto
import com.rarible.protocol.order.core.converters.dto.OrderActivityConverter
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.core.producer.ProtocolOrderPublisher
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class OnExchangeLogEventListener(
    private val eventPublisher: ProtocolOrderPublisher,
    private val orderActivityConverter: OrderActivityConverter
) : OnLogEventListener {
    private val logger = LoggerFactory.getLogger(javaClass)

    override val topics = ItemType.values().flatMap { it.topic }

    override fun onLogEvent(logEvent: LogEvent): Mono<Void> = mono {
        logger.info(
            "Order log event: id=${logEvent.id}, dataType=${logEvent.data::class.java.simpleName}"
        )
        convert(logEvent)?.let { eventPublisher.publish(it) }
    }.then()

    private suspend fun convert(source: LogEvent): OrderActivityDto? {
        if (source.status != LogEventStatus.CONFIRMED) {
            return null
        }
        val result = if (
            (source.data as? OrderSideMatch)?.side == OrderSide.LEFT
            || source.data is OnChainOrder
            || source.data is OrderCancel
        ) {
            ActivityResult.History(source)
        } else {
            null
        }
        return result?.let { orderActivityConverter.convert(it) }
    }
}
