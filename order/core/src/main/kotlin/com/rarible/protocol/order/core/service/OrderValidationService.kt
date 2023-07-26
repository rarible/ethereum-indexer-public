package com.rarible.protocol.order.core.service

import com.rarible.protocol.order.core.metric.OrderValidationMetrics
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.validator.OrderStateValidator
import org.springframework.stereotype.Component

@Component
class OrderValidationService(
    private val validators: List<OrderStateValidator>,
    private val orderValidationMetrics: OrderValidationMetrics
) {
    suspend fun validateState(order: Order) {
        validators.forEach {
            try {
                if (it.supportsValidation(order)) {
                    it.validate(order)
                    orderValidationMetrics.onOrderValidationSuccess(order.platform, it.type)
                }
            } catch (e: Exception) {
                orderValidationMetrics.onOrderValidationFail(order.platform, it.type)
                throw e
            }
        }
    }
}
