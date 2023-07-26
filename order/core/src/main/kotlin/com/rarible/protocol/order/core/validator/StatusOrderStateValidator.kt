package com.rarible.protocol.order.core.validator

import com.rarible.protocol.order.core.exception.ValidationApiException
import com.rarible.protocol.order.core.misc.orderOffchainEventMarks
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.core.model.order.logger
import com.rarible.protocol.order.core.service.OrderUpdateService
import org.springframework.stereotype.Component

@Component
class StatusOrderStateValidator(
    private val orderUpdateService: OrderUpdateService
) : OrderStateValidator {

    override val type = "status"

    override fun supportsValidation(order: Order) = true

    override suspend fun validate(order: Order) {
        if (order.status !== OrderStatus.ACTIVE) {
            logger.warn("Order validation error: hash=${order.hash}, status=${order.status}")
            orderUpdateService.update(order.hash, orderOffchainEventMarks())
            throw ValidationApiException("order ${order.platform}:${order.hash} is not active")
        }
    }
}
