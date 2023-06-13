package com.rarible.protocol.order.api.service.order.validation

import com.rarible.protocol.order.core.model.Order

interface OrderStateValidator {
    suspend fun validate(order: Order)
}