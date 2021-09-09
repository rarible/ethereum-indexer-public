package com.rarible.protocol.order.core.repository.order

import com.rarible.core.common.convert
import com.rarible.protocol.dto.OrderUpdateEventDto
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.producer.ProtocolOrderPublisher
import org.slf4j.LoggerFactory
import org.springframework.core.convert.ConversionService
import java.util.*

class NotifiableOrderRepositoryDecorator(
    private val delegate: OrderRepository,
    private val publisher: ProtocolOrderPublisher,
    private val conversionService: ConversionService
) : OrderRepository by delegate {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun save(order: Order, previousOrderVersion: Order?): Order {
        val saved = delegate.save(order, previousOrderVersion)

        if (saved != previousOrderVersion) {
            val updateEvent = OrderUpdateEventDto(
                eventId = UUID.randomUUID().toString(),
                orderId = saved.hash.toString(),
                order = conversionService.convert(saved)
            )
            publisher.publish(updateEvent)
            logger.info("Event published: $updateEvent")
        }
        return saved
    }
}
