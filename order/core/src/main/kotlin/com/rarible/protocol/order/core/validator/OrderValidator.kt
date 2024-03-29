package com.rarible.protocol.order.core.validator

import com.rarible.protocol.order.core.model.Order

interface OrderValidator {
    val type: String

    suspend fun validate(order: Order)

    fun supportsValidation(order: Order): Boolean
}
