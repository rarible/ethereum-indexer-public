package com.rarible.protocol.order.listener.service.order

import com.rarible.core.task.TaskHandler
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class CancelOpenSeaSmallOrdersTaskHandler(
    private val orderRepository: OrderRepository,
    private val orderUpdateService: OrderUpdateService,
    private val properties: OrderIndexerProperties,
) : TaskHandler<String> {

    val logger: Logger = LoggerFactory.getLogger(CancelOpenSeaSmallOrdersTaskHandler::class.java)

    override val type: String
        get() = CANCEL_SEAPORT_SMALL_ORDERS

    override fun runLongTask(from: String?, param: String): Flow<String> {
        val status = OrderStatus.valueOf(param)
        logger.info("Start $CANCEL_SEAPORT_SMALL_ORDERS task with $status param from $from")
        return orderRepository
            .findAll(Platform.OPEN_SEA, status, fromHash = from?.let { Word.apply(it) })
            .filter { isSmallMakePrice(it) }
            .map { updateOrder(it) }
    }

    private fun isSmallMakePrice(order: Order): Boolean {
        val sum =
            (order.makePrice ?: BigDecimal.ZERO) * BigDecimal.valueOf(1, -18) * order.make.value.value.toBigDecimal()

        if (order.type == OrderType.SEAPORT_V1 && sum.toInt() <= properties.minSeaportMakeWeiPrice) {
            return true
        }
        return false
    }

    private suspend fun updateOrder(order: Order): String {
        orderUpdateService.update(order.hash)
        return order.hash.toString()
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(CancelOpenSeaSmallOrdersTaskHandler::class.java)
        const val CANCEL_SEAPORT_SMALL_ORDERS = "CANCEL_SEAPORT_SMALL_ORDERS"
    }
}