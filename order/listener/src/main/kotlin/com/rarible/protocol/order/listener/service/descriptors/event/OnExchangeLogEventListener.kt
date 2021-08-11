package com.rarible.protocol.order.listener.service.descriptors.event

import com.rarible.ethereum.listener.log.OnLogEventListener
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.contracts.exchange.v1.BuyEvent
import com.rarible.protocol.contracts.exchange.v1.CancelEvent as CancelEventV1
import com.rarible.protocol.contracts.exchange.v2.events.CancelEventDeprecated
import com.rarible.protocol.contracts.exchange.v2.events.CancelEvent as CancelEventV2
import com.rarible.protocol.contracts.exchange.v2.events.MatchEvent
import com.rarible.protocol.contracts.exchange.v2.events.MatchEventDeprecated
import com.rarible.protocol.contracts.exchange.wyvern.OrderCancelledEvent
import com.rarible.protocol.contracts.exchange.wyvern.OrdersMatchedEvent
import com.rarible.protocol.dto.OrderActivityDto
import com.rarible.protocol.order.core.converters.dto.OrderActivityConverter
import com.rarible.protocol.order.core.model.ActivityResult
import com.rarible.protocol.order.core.model.OrderCancel
import com.rarible.protocol.order.core.model.OrderSide
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.core.producer.ProtocolOrderPublisher
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class OnExchangeLogEventListener(
    private val eventPublisher: ProtocolOrderPublisher,
    private val orderActivityConverter: OrderActivityConverter
) : OnLogEventListener  {
    private val logger = LoggerFactory.getLogger(javaClass)

    override val topics = listOf(
        BuyEvent.id(),
        CancelEventV1.id(),
        MatchEventDeprecated.id(),
        MatchEvent.id(),
        OrdersMatchedEvent.id(),
        OrderCancelledEvent.id(),
        CancelEventDeprecated.id(),
        CancelEventV2.id()
    )

    override fun onLogEvent(logEvent: LogEvent): Mono<Void> = mono {
        logger.info(
            "Order log event: id=${logEvent.id}, dataType=${logEvent.data::class.java.simpleName}"
        )
        convert(logEvent)?.let { eventPublisher.publish(it) }
    }.then()

    private suspend fun convert(source: LogEvent): OrderActivityDto? {
        val result = if (source.status == LogEventStatus.CONFIRMED &&
            (((source.data as? OrderSideMatch)?.side == OrderSide.LEFT) || (source.data is OrderCancel))
        ) {
            ActivityResult.History(source)
        } else {
            null
        }
        return result?.let { orderActivityConverter.convert(it) }
    }
}
