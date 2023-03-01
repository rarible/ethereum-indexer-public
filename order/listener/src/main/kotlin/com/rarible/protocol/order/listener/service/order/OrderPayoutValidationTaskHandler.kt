package com.rarible.protocol.order.listener.service.order

import com.rarible.core.common.nowMillis
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.dto.OrderUpdateEventDto
import com.rarible.protocol.dto.offchainEventMark
import com.rarible.protocol.order.core.converters.dto.OrderDtoConverter
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.producer.ProtocolOrderPublisher
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.validator.PayoutValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
class OrderPayoutValidationTaskHandler(
    private val orderRepository: OrderRepository,
    private val orderDtoConverter: OrderDtoConverter,
    private val publisher: ProtocolOrderPublisher,
) : TaskHandler<Long> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override val type = "ORDER_PAYOUT_VALIDATION_TASK"

    override fun runLongTask(from: Long?, param: String): Flow<Long> = flow {
        var updatedAt = from?.let { Instant.ofEpochMilli(it) } ?: nowMillis()
        do {
            val nextUpdatedAt = next(updatedAt)
            nextUpdatedAt?.let {
                emit(updatedAt.toEpochMilli())
                updatedAt = nextUpdatedAt
            }
        } while (nextUpdatedAt != null)
    }

    private suspend fun next(updatedAt: Instant): Instant? {
        val criteria = Criteria()
            .and(Order::platform.name).isEqualTo(Platform.RARIBLE)
            .and(Order::status.name).inValues(OrderStatus.ACTIVE, OrderStatus.INACTIVE, OrderStatus.NOT_STARTED)
            .and(Order::lastUpdateAt.name).lt(updatedAt)

        val query = Query(criteria)
            //.withHint(OrderRepositoryIndexes.SELL_ORDERS_PLATFORM_STATUS_DEFINITION.indexKeys)
            .with(Sort.by(Order::lastUpdateAt.name, "_id"))
            .limit(1000)

        val orders = orderRepository.search(query)

        orders.filter { !PayoutValidator.arePayoutsValid(it.data) }
            .forEach { cancelOrder(it) }

        return orders.lastOrNull()?.lastUpdateAt
    }

    private suspend fun cancelOrder(order: Order) {
        val cancelled = order
            .withCancel(true)
            .withUpdatedStatus()

        val saved = orderRepository.save(cancelled)
        logger.info("Order ${saved.id} cancelled due to incorrect payouts")

        val updateEvent = OrderUpdateEventDto(
            eventId = UUID.randomUUID().toString(),
            orderId = saved.id.toString(),
            order = orderDtoConverter.convert(saved),
            eventTimeMarks = offchainEventMark("indexer-out_order")
        )
        publisher.publish(updateEvent)
    }

}