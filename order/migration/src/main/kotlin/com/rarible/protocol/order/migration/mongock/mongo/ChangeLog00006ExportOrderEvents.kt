package com.rarible.protocol.order.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.protocol.dto.OrderUpdateEventDto
import com.rarible.protocol.order.core.converters.dto.OrderDtoConverter
import com.rarible.protocol.order.core.producer.ProtocolOrderPublisher
import com.rarible.protocol.order.core.repository.order.OrderRepository
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.util.*

@ChangeLog(order = "00006")
class ChangeLog00006ExportOrderEvents {
    @ChangeSet(id = "ChangeLog00006ExportOrderEvents.exportAllOrdersEvents", order = "1", author = "protocol")
    fun createIndexForAll(
        @NonLockGuarded orderRepository: OrderRepository,
        @NonLockGuarded publisher: ProtocolOrderPublisher,
        @NonLockGuarded orderDtoConverter: OrderDtoConverter
    ) = runBlocking {
        val logger = LoggerFactory.getLogger(javaClass)

        logger.info("--- Start all orders events publishing")
        var counter = 0L

        orderRepository.findAll().collect { order ->
            try {
                val updateEvent = OrderUpdateEventDto(
                    eventId = UUID.randomUUID().toString(),
                    orderId = order.hash.toString(),
                    order = orderDtoConverter.convert(order)
                )
                publisher.publish(updateEvent)

                counter++

                if (counter % 50000L == 0L) {
                    logger.info("Published $counter events")
                }
            } catch (ex: Exception) {
                logger.error("Can't publish event for ${order.hash}")
            }
        }
        logger.info("--- All $counter orders events were published")
    }
}
