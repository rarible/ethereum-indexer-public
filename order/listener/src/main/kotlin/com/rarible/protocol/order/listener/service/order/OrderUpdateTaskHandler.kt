package com.rarible.protocol.order.listener.service.order

import com.rarible.core.task.TaskHandler
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.listener.configuration.OrderListenerProperties
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.time.delay
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.*

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
        return orderRepository.findAllBeforeLastUpdateAt(from?.let { Date(it) })
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
        val (_, updated) = orderUpdateService.updateMakeStockFull(hash = order.hash)
        if (updated) {
            logger.info("Order ${order.hash} has been updated by task '$ORDER_UPDATE'")
            delay(Duration.ofMillis(properties.publishTaskDelayMs))
        }
    }

    companion object {
        const val ORDER_UPDATE = "ORDER_UPDATE"
    }
}

private fun <T> Flow<T>.chunked(size: Int): Flow<List<T>> = flow {
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
