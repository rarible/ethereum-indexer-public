package com.rarible.protocol.order.listener.service.opensea

import com.rarible.core.common.EventTimeMark
import com.rarible.core.common.EventTimeMarks
import com.rarible.core.kafka.RaribleKafkaEventHandler
import com.rarible.opensea.subscriber.model.OpenseaEvent
import com.rarible.opensea.subscriber.model.OpenseaItemCancelled
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderCancelService
import com.rarible.protocol.order.listener.misc.ForeignOrderMetrics
import io.daonomic.rpc.domain.Word
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component


@Component
class OpenseaEventHandler(
    private val orderCancelService: OrderCancelService,
    private val orderRepository: OrderRepository,
    private val foreignOrderMetrics: ForeignOrderMetrics,
) : RaribleKafkaEventHandler<OpenseaEvent> {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handle(event: OpenseaEvent) {
        val timeMarks = event.eventTimeMarks.let { marks ->
            EventTimeMarks(
                source = marks.source,
                marks = marks.marks.map { EventTimeMark(it.name, it.date) }
            )
        }
        when (val payload = event.payload) {
            is OpenseaItemCancelled -> handleCancelEvent(payload, timeMarks)
        }
    }

    private suspend fun handleCancelEvent(event: OpenseaItemCancelled, eventTimeMarks: EventTimeMarks) {
        val id = Word.apply(event.orderHash)
        val order = orderRepository.findById(id) ?: return
        when (order.status) {
            OrderStatus.ACTIVE -> doCancel(order, eventTimeMarks)
            OrderStatus.CANCELLED -> logger.info("Order ${order.id.hash} is already cancelled")
            else -> logger.debug("Ignore order ${order.id.hash}")
        }
    }

    private suspend fun doCancel(order: Order, eventTimeMarks: EventTimeMarks) {
        logger.info("Order ${order.id.hash} will be cancelled")
        foreignOrderMetrics.onOrderEventHandled(platform = order.platform, "cancel_stream")
        orderCancelService.cancelOrder(
            id = order.id.hash,
            eventTimeMarksDto = eventTimeMarks,
        )
    }
}
