package com.rarible.protocol.order.listener.service.order

import com.rarible.core.task.TaskHandler
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.listener.configuration.OrderListenerProperties
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.time.delay
import org.jboss.logging.Logger
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.*

@Component
class OrderUpdateTaskHandler(
    private val orderRepository: OrderRepository,
    private val orderUpdateService: OrderUpdateService,
    private val properties: OrderListenerProperties
) : TaskHandler<Long> {
    private val logger = Logger.getLogger(javaClass)

    override val type: String
        get() = ORDER_UPDATE

    override suspend fun isAbleToRun(param: String): Boolean {
        return true
    }

    override fun runLongTask(from: Long?, param: String): Flow<Long> {
        return orderRepository.findAllBeforeLastUpdateAt(from?.let { Date(it) })
            .map { order ->
                handleOrder(order)
                order.lastUpdateAt.toEpochMilli()
            }
    }

    private suspend fun handleOrder(order: Order) {
        orderUpdateService.updateMakeStock(hash = order.hash)
        logger.info("Order ${order.hash} has been updated by task '$ORDER_UPDATE'")
        delay(Duration.ofMillis(properties.publishTaskDelayMs))
    }

    companion object {
        const val ORDER_UPDATE = "ORDER_UPDATE"
    }
}