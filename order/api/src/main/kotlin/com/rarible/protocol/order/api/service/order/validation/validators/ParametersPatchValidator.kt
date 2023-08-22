package com.rarible.protocol.order.api.service.order.validation.validators

import com.rarible.protocol.dto.EthereumOrderUpdateApiErrorDto
import com.rarible.protocol.order.api.form.OrderForm
import com.rarible.protocol.order.api.service.order.validation.OrderFormValidator
import com.rarible.protocol.order.core.exception.OrderUpdateException
import com.rarible.protocol.order.core.repository.order.OrderRepository
import org.springframework.stereotype.Component

@Component
class ParametersPatchValidator(
    private val orderRepository: OrderRepository
) : OrderFormValidator {

    override suspend fun validate(form: OrderForm) {
        val order = orderRepository.findById(form.hash) ?: return

        if (order.cancelled) {
            throw OrderUpdateException("Order is cancelled", EthereumOrderUpdateApiErrorDto.Code.ORDER_CANCELED)
        }
        if (order.data != form.data) {
            throw OrderUpdateException(
                "Order update failed ('data' changed)", EthereumOrderUpdateApiErrorDto.Code.ORDER_INVALID_UPDATE
            )
        }
        if (order.start != form.start) {
            throw OrderUpdateException(
                "Order update failed ('start' changed)", EthereumOrderUpdateApiErrorDto.Code.ORDER_INVALID_UPDATE
            )
        }
        if (order.end != form.end) {
            throw OrderUpdateException(
                "Order update failed ('end' changed)", EthereumOrderUpdateApiErrorDto.Code.ORDER_INVALID_UPDATE
            )
        }
        if (order.taker != form.taker) {
            throw OrderUpdateException(
                "Order update failed ('taker' changed)", EthereumOrderUpdateApiErrorDto.Code.ORDER_INVALID_UPDATE
            )
        }
        if (form.make.value < order.make.value) {
            throw OrderUpdateException(
                "Order update failed ('make.value' less then current)",
                EthereumOrderUpdateApiErrorDto.Code.ORDER_INVALID_UPDATE
            )
        }

        val newMaxTake = form.make.value * order.take.value / order.make.value
        if (newMaxTake < form.take.value) {
            throw OrderUpdateException(
                "Order update failed ('take.value' greater than maximum available: $newMaxTake)",
                EthereumOrderUpdateApiErrorDto.Code.ORDER_INVALID_UPDATE
            )
        }
    }
}
