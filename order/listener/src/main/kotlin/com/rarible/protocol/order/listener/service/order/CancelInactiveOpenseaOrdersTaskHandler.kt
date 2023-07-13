package com.rarible.protocol.order.listener.service.order

import com.rarible.core.task.RunTask
import com.rarible.protocol.order.core.misc.orderTaskEventMarks
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.core.service.updater.CancelInactiveOrderUpdater
import com.rarible.protocol.order.listener.configuration.OrderListenerProperties
import org.springframework.stereotype.Component

@Component
class CancelInactiveOpenseaOrdersTaskHandler(
    orderRepository: OrderRepository,
    properties: OrderListenerProperties,
    private val cancelInactiveOrderUpdater: CancelInactiveOrderUpdater,
    private val orderUpdateService: OrderUpdateService
) : AbstractOrderUpdateStatusTaskHandler(
    orderRepository,
    properties
) {

    private val seaport1_4param =
        TaskParam(
            status = OrderStatus.INACTIVE,
            platform = Platform.OPEN_SEA,
            listedAfter = 1677628800 // 01.03.2023 - we need to cancel only Seaport 1.4+ orders
        )

    override fun getAutorunParams(): List<RunTask> {
        return listOf(
            RunTask(objectMapper.writeValueAsString(seaport1_4param))
        )
    }

    override val type = "CANCEL_INACTIVE_OPENSEA_ORDERS"

    override suspend fun handleOrder(order: Order) {
        val eventTimeMarks = orderTaskEventMarks()
        cancelInactiveOrderUpdater.update(order)
        orderUpdateService.update(order.hash, eventTimeMarks)
    }
}
