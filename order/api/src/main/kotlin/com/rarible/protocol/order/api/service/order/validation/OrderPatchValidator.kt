package com.rarible.protocol.order.api.service.order.validation

import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderVersion

interface OrderPatchValidator {
    suspend fun validate(order: Order, update: OrderVersion)
}
