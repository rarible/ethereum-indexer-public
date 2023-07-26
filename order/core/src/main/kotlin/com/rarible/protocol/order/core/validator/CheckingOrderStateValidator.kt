package com.rarible.protocol.order.core.validator

import com.rarible.core.logging.addToMdc
import com.rarible.protocol.order.core.exception.OrderDataException
import com.rarible.protocol.order.core.misc.orderOffchainEventMarks
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.model.itemId
import com.rarible.protocol.order.core.model.nft
import com.rarible.protocol.order.core.model.order.logger
import com.rarible.protocol.order.core.service.OrderCancelService
import com.rarible.protocol.order.core.service.OrderStateCheckService

class CheckingOrderStateValidator(
    private val orderStateCheckService: OrderStateCheckService,
    private val orderCancelService: OrderCancelService,
    private val platform: Platform,
) : OrderStateValidator {

    override val type = "aggregation"

    override fun supportsValidation(order: Order) = order.platform == platform

    override suspend fun validate(order: Order) {
        val eventTimeMarks = orderOffchainEventMarks()
        val active = try {
            orderStateCheckService.isActiveOrder(order)
        } catch (e: Exception) {
            logger.error(
                "Error during getting $platform order hash=${order.hash}, " +
                    "itemId=${order.nft().type.itemId}, status: $e", e
            )
            true
        }
        if (!active) {
            addToMdc("orderType" to order.type.name) {
                logger.info("Unexpected order cancellation: ${order.type}:${order.hash}")
            }
            orderCancelService.cancelOrder(id = order.hash, eventTimeMarksDto = eventTimeMarks)
            throw OrderDataException("order $platform:${order.hash} is not active")
        }
    }
}
