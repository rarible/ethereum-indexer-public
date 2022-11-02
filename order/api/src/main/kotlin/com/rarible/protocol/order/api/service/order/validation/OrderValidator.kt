package com.rarible.protocol.order.api.service.order.validation

import com.rarible.protocol.order.api.service.order.validation.validators.OrderSignatureValidator
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderVersion
import org.springframework.stereotype.Service

@Service
class OrderValidator(
    private val orderSignatureValidators: List<OrderSignatureValidator>,
    private val orderPatchValidators: List<OrderPatchValidator>
) : OrderVersionValidator, OrderPatchValidator {

    override suspend fun validate(orderVersion: OrderVersion) {
        orderSignatureValidators.forEach { it.validate(orderVersion) }
    }

    override suspend fun validate(order: Order, update: OrderVersion) {
        orderPatchValidators.forEach { it.validate(order, update) }
    }
}