package com.rarible.protocol.order.listener.service.order

import com.rarible.core.task.RunTask
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.core.repository.order.OrderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.lte
import org.springframework.stereotype.Component
import java.time.Instant

// One-off job for fixing orders with end = 0 and status = ENDED
@Component
class OrderStatusEndUpdateTaskHandler(
    private val orderRepository: OrderRepository,
    private val mongo: ReactiveMongoTemplate
) : TaskHandler<String> {

    override val type: String
        get() = "ORDER_STATUS_END_UPDATE"

    override fun getAutorunParams(): List<RunTask> {
        return listOf(RunTask("", null))
    }

    override fun runLongTask(from: String?, param: String): Flow<String> {
        logger.info("Started finding ENDED order with end = 0 from ${from ?: Instant.now().epochSecond.toString()}")

        // It's covered by status_1_end_1_start_1 index
        val query = Query(Criteria().andOperator(
            Order::status isEqualTo OrderStatus.ENDED,
            Order::end isEqualTo 0,
            Order::start lte (from?.let { from.toLong() } ?: Instant.now().epochSecond)
        )).with(Sort.by(Sort.Direction.DESC, Order::start.name))

        return mongo.query<Order>().matching(query).all().asFlow().map { order ->
            val updated = orderRepository.save(order.withUpdatedStatus())
            logger.info("Save order ${updated.hash.prefixed()} with status: ${updated.status} and start: ${updated.start}")
            updated.start?.toString() ?: Instant.now().epochSecond.toString()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OrderStatusEndUpdateTaskHandler::class.java)
    }
}
