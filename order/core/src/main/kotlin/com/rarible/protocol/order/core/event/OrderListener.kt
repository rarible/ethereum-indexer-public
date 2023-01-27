package com.rarible.protocol.order.core.event

import com.rarible.protocol.dto.EventTimeMarksDto
import com.rarible.protocol.dto.OrderUpdateEventDto
import com.rarible.protocol.dto.add
import com.rarible.protocol.dto.offchainEventMark
import com.rarible.protocol.order.core.converters.dto.OrderDtoConverter
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.producer.ProtocolOrderPublisher
import org.springframework.stereotype.Component
import java.util.*

@Component
class OrderListener(
    private val eventPublisher: ProtocolOrderPublisher,
    private val orderDtoConverter: OrderDtoConverter
) {

    suspend fun onOrder(order: Order, eventTimeMarks: EventTimeMarksDto?) {
        val markName = "indexer-out_order"
        val marks = eventTimeMarks?.add(markName) ?: offchainEventMark(markName)

        val updateEvent = OrderUpdateEventDto(
            eventId = order.lastEventId ?: UUID.randomUUID().toString(),
            orderId = order.id.toString(),
            order = orderDtoConverter.convert(order),
            eventTimeMarks = marks
        )
        eventPublisher.publish(updateEvent)
    }
}
