package com.rarible.protocol.order.api.service.order.validation.validators

import com.rarible.protocol.order.api.service.order.validation.OrderStateValidator
import com.rarible.protocol.order.core.model.Order
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

@Primary
@Component
class CompositeOrderStateValidator(
    private val validators: List<OrderStateValidator>
) : OrderStateValidator {
    override suspend fun validate(order: Order) {
        validators.forEach { it.validate(order) }
    }
}