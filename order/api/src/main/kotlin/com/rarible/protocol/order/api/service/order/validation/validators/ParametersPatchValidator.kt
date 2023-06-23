package com.rarible.protocol.order.api.service.order.validation.validators

import com.rarible.protocol.dto.EthereumOrderUpdateApiErrorDto
import com.rarible.protocol.order.api.service.order.validation.OrderPatchValidator
import com.rarible.protocol.order.core.exception.OrderUpdateException
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderVersion
import org.springframework.stereotype.Component

@Component
class ParametersPatchValidator : OrderPatchValidator {

    override suspend fun validate(order: Order, update: OrderVersion) {
        if (order.cancelled) {
            throw OrderUpdateException("Order is cancelled", EthereumOrderUpdateApiErrorDto.Code.ORDER_CANCELED)
        }
        if (order.data != update.data) {
            throw OrderUpdateException(
                "Order update failed ('data' changed)", EthereumOrderUpdateApiErrorDto.Code.ORDER_INVALID_UPDATE
            )
        }
        if (order.start != update.start) {
            throw OrderUpdateException(
                "Order update failed ('start' changed)", EthereumOrderUpdateApiErrorDto.Code.ORDER_INVALID_UPDATE
            )
        }
        if (order.end != update.end) {
            throw OrderUpdateException(
                "Order update failed ('end' changed)", EthereumOrderUpdateApiErrorDto.Code.ORDER_INVALID_UPDATE
            )
        }
        if (order.taker != update.taker) {
            throw OrderUpdateException(
                "Order update failed ('taker' changed)", EthereumOrderUpdateApiErrorDto.Code.ORDER_INVALID_UPDATE
            )
        }
        if (update.make.value < order.make.value) {
            throw OrderUpdateException(
                "Order update failed ('make.value' less then current)",
                EthereumOrderUpdateApiErrorDto.Code.ORDER_INVALID_UPDATE
            )
        }

        val newMaxTake = update.make.value * order.take.value / order.make.value
        if (newMaxTake < update.take.value) {
            throw OrderUpdateException(
                "Order update failed ('take.value' greater than maximum available: $newMaxTake)",
                EthereumOrderUpdateApiErrorDto.Code.ORDER_INVALID_UPDATE
            )
        }
    }
}