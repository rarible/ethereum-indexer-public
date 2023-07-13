package com.rarible.protocol.order.core.validator

import com.rarible.protocol.order.core.exception.ValidationApiException
import com.rarible.protocol.order.core.misc.orderOffchainEventMarks
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.order.logger
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.core.service.approve.ApproveService
import org.springframework.stereotype.Component

@Component
class ApprovalsOrderStateValidator(
    private val approveService: ApproveService,
    private val orderUpdateService: OrderUpdateService
) : OrderStateValidator {

    override val type = "approval"

    override fun supportsValidation(order: Order) = true

    override suspend fun validate(order: Order) {
        val eventTimeMarks = orderOffchainEventMarks()
        val approved = approveService.checkOnChainApprove(order.maker, order.make.type, order.platform)
        val correctAllowance = approveService.checkOnChainErc20Allowance(order.maker, order.make)
        if (!approved || !correctAllowance) {
            logger.warn("Order validation error: hash=${order.hash}, approved=$approved, correctAllowance=$correctAllowance")
            orderUpdateService.updateApproval(
                order = order,
                approved = false,
                eventTimeMarks = eventTimeMarks
            )
            throw ValidationApiException("order ${order.platform}:${order.hash} is not approved")
        }
    }
}