package com.rarible.protocol.order.listener.service.order

import com.rarible.core.task.TaskHandler
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.misc.orderTaskEventMarks
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.Order.Id.Companion.toOrderId
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class CancelOpenSeaSmallOrdersTaskHandler(
    private val orderRepository: OrderRepository,
    private val orderUpdateService: OrderUpdateService,
    private val properties: OrderIndexerProperties,
) : TaskHandler<String> {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override val type = "CANCEL_SEAPORT_SMALL_ORDERS"

    override fun runLongTask(from: String?, param: String): Flow<String> {
        val status = OrderStatus.valueOf(param)
        logger.info("Start $type task with $status param from $from")
        return orderRepository
            .findAll(Platform.OPEN_SEA, status, fromHash = from?.toOrderId()?.hash)
            .filter { isSmallMakePrice(it) }
            .map { updateOrder(it) }
    }

    private fun isSmallMakePrice(order: Order): Boolean {
        if (order.makePrice != null && order.makePrice!! <= properties.minSeaportMakePrice) {
            return true
        }
        return false
    }

    private suspend fun updateOrder(order: Order): String {
        logger.info("Cancel order ${order.id} as Seaport with small price by job")
        orderUpdateService.update(order.hash, orderTaskEventMarks())
        return order.id.toString()
    }
}
