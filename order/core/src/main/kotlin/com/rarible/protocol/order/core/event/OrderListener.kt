package com.rarible.protocol.order.core.event

import com.rarible.core.common.EventTimeMarks
import com.rarible.protocol.dto.OrderUpdateEventDto
import com.rarible.protocol.order.core.converters.dto.OrderDtoConverter
import com.rarible.protocol.order.core.misc.addIndexerOut
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

    suspend fun onOrder(order: Order, eventTimeMarks: EventTimeMarks, useLastEventId: Boolean = true) {
        val eventId = if (useLastEventId) order.lastEventId else null
        val updateEvent = OrderUpdateEventDto(
            eventId = eventId ?: UUID.randomUUID().toString(),
            orderId = order.id.toString(),
            order = orderDtoConverter.convert(order),
            eventTimeMarks = eventTimeMarks.addIndexerOut().toDto()
        )
        eventPublisher.publish(updateEvent)
    }
}
