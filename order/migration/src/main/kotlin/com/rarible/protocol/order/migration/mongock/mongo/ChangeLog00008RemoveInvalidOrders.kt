package com.rarible.protocol.order.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.core.common.optimisticLock
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.OrderUpdateEventDto
import com.rarible.protocol.order.core.converters.dto.OrderDtoConverter
import com.rarible.protocol.order.core.misc.div
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.producer.ProtocolOrderPublisher
import com.rarible.protocol.order.core.repository.order.OrderRepository
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.delay
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import java.time.Duration
import java.util.*

@ChangeLog(order = "00008")
class ChangeLog00008RemoveInvalidOrders {
    @ChangeSet(id = "ChangeLog00008RemoveInvalidOrders.removeOrdersWithMakeValueZero", order = "1", author = "protocol")
    fun removeOrdersWithMakeValueZero(
        @NonLockGuarded orderRepository: OrderRepository,
        @NonLockGuarded publisher: ProtocolOrderPublisher,
        @NonLockGuarded template: ReactiveMongoTemplate
    ) = runBlocking {
        val logger = LoggerFactory.getLogger(javaClass)

        logger.info("--- Start canceling orders ")
        var counter = 0L

        val queue = Query().addCriteria(
            Order::make / Asset::value isEqualTo EthUInt256.ZERO
        )
        val invalidOrders = orderRepository.search(queue).filter { isInvalidOrder(it) }

        invalidOrders.forEach {
            optimisticLock {
                val order = orderRepository.findById(it.hash)
                if (order != null) {
                    val canceledOrder = order.copy(cancelled = true)
                    template.save(canceledOrder).awaitFirst()

                    val updateEvent = OrderUpdateEventDto(
                        eventId = UUID.randomUUID().toString(),
                        orderId = canceledOrder.hash.toString(),
                        order = OrderDtoConverter.convert(canceledOrder)
                    )
                    publisher.publish(updateEvent)

                    logger.info("Order ${canceledOrder.hash} was canceled")
                }
            }
            counter++
        }
        logger.info("--- All $counter orders was canceled")

        delay(Duration.ofMinutes(15).toMillis())

        logger.info("--- Start removing orders ")

        counter = 0L
        invalidOrders.forEach {
            orderRepository.remove(it.hash)
            logger.info("Order ${it.hash} was removed")
            counter++
        }
        logger.info("--- All $counter orders was removed")
    }

    private fun isInvalidOrder(order: Order): Boolean {
        return order.make.value == EthUInt256.ZERO
    }
}
