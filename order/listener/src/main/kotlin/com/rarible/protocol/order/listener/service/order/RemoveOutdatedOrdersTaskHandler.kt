package com.rarible.protocol.order.listener.service.order

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.order.core.misc.orderTaskEventMarks
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderDataVersion
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderCancelService
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class RemoveOutdatedOrdersTaskHandler(
    private val orderRepository: OrderRepository,
    private val orderCancelService: OrderCancelService,
    private val objectMapper: ObjectMapper,
) : TaskHandler<String> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override val type = TYPE

    override fun runLongTask(from: String?, param: String): Flow<String> {
        val params = objectMapper.readValue(param, RemoveOutdatedOrdersTaskParams::class.java)
        logger.info("Start $type task with $param param")
        return orderRepository
            .findAll(params.platform, params.status, fromHash = from?.let { Word.apply(from) })
            .filter { isCandidateToBeCancelled(params, it) }
            .map { updateOrder(it) }
    }

    private fun isCandidateToBeCancelled(params: RemoveOutdatedOrdersTaskParams, order: Order): Boolean {
        val matchType = order.type == params.type
        val matchVersion = order.data.version == params.version
        val matchContract = params.contractAddress == null || params.contractAddress == order.data.contract()
        return matchType && matchVersion && matchContract
    }

    private suspend fun updateOrder(order: Order): String {
        logger.info("Canceling order: ${order.id}")
        orderCancelService.cancelOrder(order.hash, orderTaskEventMarks())
        return order.id.toString()
    }

    companion object {
        const val TYPE = "REMOVE_OUTDATED_ORDERS"
    }
}

// {"platform":"LOOKSRARE","status":"ACTIVE","type":"LOOKSRARE","version":"LOOKSRARE_V1","contractAddress":null}
data class RemoveOutdatedOrdersTaskParams(
    val platform: Platform,
    val status: OrderStatus,
    val type: OrderType,
    val version: OrderDataVersion,
    val contractAddress: String?,
)
