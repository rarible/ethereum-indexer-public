package com.rarible.protocol.order.listener.service.order

import com.rarible.core.common.ifNotBlank
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.order.core.misc.orderTaskEventMarks
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.listener.configuration.OrderListenerProperties
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.time.delay
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.Date

@Component
class OrderUpdateTaskHandler(
    private val orderRepository: OrderRepository,
    private val orderUpdateService: OrderUpdateService,
    private val properties: OrderListenerProperties
) : TaskHandler<Long> {
    private val logger = LoggerFactory.getLogger(javaClass)

    override val type: String
        get() = ORDER_UPDATE

    override suspend fun isAbleToRun(param: String): Boolean {
        return true
    }

    override fun runLongTask(from: Long?, param: String): Flow<Long> {
        val status = param.ifNotBlank()?.let { OrderStatus.valueOf(it) }
        return orderRepository.findAllBeforeLastUpdateAt(from?.let { Date(it) }, status, null)
            .chunked(properties.parallelOrderUpdateStreams)
            .map { orders ->
                coroutineScope {
                    orders.map {
                        async { handleOrder(it) }
                    }.awaitAll()
                }
                orders.minOf { it.lastUpdateAt.toEpochMilli() }
            }
    }

    private suspend fun handleOrder(order: Order) {
        val (updatedOrder, updated) = orderUpdateService.updateMakeStockFull(order.hash, null, orderTaskEventMarks())
        if (updated) {
            logger.info("Order ${updatedOrder?.id} has been updated by task '$ORDER_UPDATE', oldStatus=${order.status}, newStatus=${updatedOrder?.status}")
            delay(Duration.ofMillis(properties.publishTaskDelayMs))
        }
    }

    companion object {
        const val ORDER_UPDATE = "ORDER_UPDATE"
    }
}

fun <T> Flow<T>.chunked(size: Int): Flow<List<T>> = flow {
    var list = mutableListOf<T>()
    this@chunked.collect {
        list.add(it)
        if (list.size >= size) {
            emit(list)
            list = mutableListOf()
        }
    }
    if (list.isNotEmpty()) {
        emit(list)
    }
}
