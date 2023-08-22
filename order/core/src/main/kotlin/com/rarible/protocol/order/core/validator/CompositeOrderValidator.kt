package com.rarible.protocol.order.core.validator

import com.rarible.protocol.order.core.metric.OrderValidationMetrics
import com.rarible.protocol.order.core.model.Order
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

@Component
@Primary
class CompositeOrderValidator(
    private val validators: List<OrderValidator>,
    private val orderValidationMetrics: OrderValidationMetrics,
) : OrderValidator {

    override val type: String = "order_validator"

    override fun supportsValidation(order: Order): Boolean = true

    override suspend fun validate(order: Order) {
        validators.filter { it.supportsValidation(order) }.map {
            try {
                it.validate(order)
                orderValidationMetrics.onOrderValidationSuccess(order.platform, it.type)
            } catch (e: Exception) {
                orderValidationMetrics.onOrderValidationFail(order.platform, it.type)
                throw e
            }
        }
    }
}
