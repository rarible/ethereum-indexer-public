package com.rarible.protocol.order.listener.service.looksrare

import com.rarible.protocol.order.core.model.OrderVersion

interface LooksrareOrderValidator {
    fun validate(order: OrderVersion): Boolean
}