package com.rarible.protocol.order.api.service.order.validation.validators

import com.rarible.protocol.order.api.exceptions.OrderDataException
import com.rarible.protocol.order.api.service.order.validation.OrderStateValidator
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
    override suspend fun validate(order: Order) {
        if (order.platform == platform) {
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
                orderCancelService.cancelOrder(id = order.hash, eventTimeMarksDto = orderOffchainEventMarks())
                throw OrderDataException("order $platform:${order.hash} is not active")
            }
        }
    }
}