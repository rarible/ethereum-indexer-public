package com.rarible.protocol.order.core.event

import com.rarible.protocol.dto.OrderUpdateEventDto
import com.rarible.protocol.order.core.converters.dto.OrderDtoConverter
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.producer.ProtocolOrderPublisher
import org.springframework.stereotype.Component
import java.util.*

@Component
class OrderListener(
    private val eventPublisher: ProtocolOrderPublisher
) {
    suspend fun onOrder(order: Order) {
        val updateEvent = OrderUpdateEventDto(
            eventId = UUID.randomUUID().toString(),
            orderId = order.hash.toString(),
            order = OrderDtoConverter.convert(order)
        )
        eventPublisher.publish(updateEvent)
    }
}
