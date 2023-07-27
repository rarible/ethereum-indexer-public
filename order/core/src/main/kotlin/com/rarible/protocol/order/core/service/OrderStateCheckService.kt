package com.rarible.protocol.order.core.service

import com.rarible.protocol.order.core.model.Order

interface OrderStateCheckService {
    suspend fun isActiveOrder(order: Order): Boolean
}
