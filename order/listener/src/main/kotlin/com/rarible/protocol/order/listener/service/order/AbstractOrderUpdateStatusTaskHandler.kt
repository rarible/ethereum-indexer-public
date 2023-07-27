package com.rarible.protocol.order.listener.service.order

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.listener.configuration.OrderListenerProperties
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.takeWhile
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.Date

abstract class AbstractOrderUpdateStatusTaskHandler(
    protected val orderRepository: OrderRepository,
    protected val properties: OrderListenerProperties,
) : TaskHandler<Long> {
    protected val logger: Logger = LoggerFactory.getLogger(javaClass)

    override suspend fun isAbleToRun(param: String): Boolean {
        return true
    }

    protected abstract suspend fun handleOrder(order: Order)

    override fun runLongTask(from: Long?, param: String): Flow<Long> {
        val taskParam = objectMapper.readValue(param, TaskParam::class.java)
        val lastUpdatedAt = from?.let { Instant.ofEpochSecond(it) }

        return orderRepository.findAllBeforeLastUpdateAt(
            lastUpdatedAt?.let { Date.from(it) },
            taskParam.status,
            taskParam.platform
        ).chunked(properties.parallelOrderUpdateStreams)
            .map { orders ->
                coroutineScope {
                    orders.map {
                        async { handleOrder(it) }
                    }.awaitAll()
                }
                orders.minOf { it.lastUpdateAt.epochSecond }
            }
            .takeWhile { it > taskParam.listedAfter }
    }

    data class TaskParam(
        val status: OrderStatus,
        val platform: Platform,
        val listedAfter: Long
    )

    companion object {
        val objectMapper = ObjectMapper().registerKotlinModule()
    }
}
