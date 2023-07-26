package com.rarible.protocol.order.core.service.updater

import com.rarible.protocol.order.core.model.Order

interface CustomOrderUpdater {

    suspend fun update(order: Order): Order
}
