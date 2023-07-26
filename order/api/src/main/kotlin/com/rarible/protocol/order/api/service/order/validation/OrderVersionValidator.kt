package com.rarible.protocol.order.api.service.order.validation

import com.rarible.protocol.order.core.model.OrderVersion

interface OrderVersionValidator {
    suspend fun validate(orderVersion: OrderVersion)
}
