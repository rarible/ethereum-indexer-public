package com.rarible.protocol.order.listener.service.opensea

import com.rarible.core.common.EventTimeMark
import com.rarible.core.common.EventTimeMarks
import com.rarible.core.kafka.RaribleKafkaEventHandler
import com.rarible.core.logging.addToMdc
import com.rarible.opensea.subscriber.model.OpenseaEvent
import com.rarible.opensea.subscriber.model.OpenseaItemCancelled
import com.rarible.protocol.order.core.misc.orderOffchainEventMarks
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderCancelService
import com.rarible.protocol.order.listener.configuration.OpenseaEventProperties
import com.rarible.protocol.order.listener.misc.ForeignOrderMetrics
import io.daonomic.rpc.domain.Word
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component


@Component
class OpenseaEventHandler(
    private val orderCancelService: OrderCancelService,
    private val properties: OpenseaEventProperties,
    private val orderRepository: OrderRepository,
    private val foreignOrderMetrics: ForeignOrderMetrics,
) : RaribleKafkaEventHandler<OpenseaEvent> {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handle(event: OpenseaEvent) {
        val timeMarks = event.eventTimeMarks?.let { marks ->
            EventTimeMarks(
                source = marks.source,
                marks = marks.marks.map { EventTimeMark(it.name, it.date) }
            )
        } ?: orderOffchainEventMarks()
        when (val payload = event.payload) {
            is OpenseaItemCancelled -> handleCancelEvent(payload, timeMarks)
        }
    }

    private suspend fun handleCancelEvent(event: OpenseaItemCancelled, eventTimeMarks: EventTimeMarks) {
        val id = Word.apply(event.orderHash)
        val order = orderRepository.findById(id) ?: return
        if (order.status == OrderStatus.ACTIVE) {
            logger.warn(
                "Found canceled order but active in the database ${order.type}: $id. Will cancel: ${properties.cancelEnabled}"
            )
            foreignOrderMetrics.onOrderInconsistency(platform = order.platform, status = order.status.name)
            if (properties.cancelEnabled) {
                addToMdc("orderType" to order.type.name) {
                    logger.info("Unexpected order cancellation: ${order.type}:${order.hash}")
                }
                orderCancelService.cancelOrder(
                    id = id,
                    eventTimeMarksDto = eventTimeMarks,
                )
            }
        }
    }
}
