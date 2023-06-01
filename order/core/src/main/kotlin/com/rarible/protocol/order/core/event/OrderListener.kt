package com.rarible.protocol.order.core.event

import com.rarible.core.common.EventTimeMarks
import com.rarible.protocol.dto.OrderUpdateEventDto
import com.rarible.protocol.order.core.converters.dto.OrderDtoConverter
import com.rarible.protocol.order.core.misc.addOut
import com.rarible.protocol.order.core.misc.orderOffchainEventMarks
import com.rarible.protocol.order.core.misc.toDto
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.producer.ProtocolOrderPublisher
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class OrderListener(
    private val eventPublisher: ProtocolOrderPublisher,
    private val orderDtoConverter: OrderDtoConverter
) {

    suspend fun onOrder(order: Order, eventTimeMarks: EventTimeMarks?) {
        val updateEvent = OrderUpdateEventDto(
            eventId = order.lastEventId ?: UUID.randomUUID().toString(),
            orderId = order.id.toString(),
            order = orderDtoConverter.convert(order),
            eventTimeMarks = (eventTimeMarks ?: orderOffchainEventMarks()).addOut().toDto()
        )
        eventPublisher.publish(updateEvent)
    }
}
