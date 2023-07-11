package com.rarible.protocol.order.listener.service.order

import com.rarible.core.task.TaskHandler
import com.rarible.protocol.order.core.event.OrderListener
import com.rarible.protocol.order.core.misc.orderOffchainEventMarks
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.listener.configuration.OrderListenerProperties
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.time.delay
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.Date

@Component
class OrderPublishTaskHandler(
    private val orderRepository: OrderRepository,
    private val orderListener: OrderListener,
    private val properties: OrderListenerProperties
) : TaskHandler<Long> {

    override val type: String
        get() = ORDER_PUBLISH

    override suspend fun isAbleToRun(param: String): Boolean {
        return true
    }

    override fun runLongTask(from: Long?, param: String): Flow<Long> {
        return orderRepository.findAllBeforeLastUpdateAt(from?.let { Date(it) }, null, null)
            .map { order ->
                orderListener.onOrder(order, orderOffchainEventMarks(), false)
                delay(Duration.ofMillis(properties.publishTaskDelayMs))

                order.lastUpdateAt.toEpochMilli()
            }
    }

    companion object {
        const val ORDER_PUBLISH = "ORDER_PUBLISH"
    }
}
