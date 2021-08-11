package com.rarible.protocol.order.core.repository.order

import com.rarible.core.common.nowMillis
import com.rarible.protocol.order.core.model.Order

class OptimizedOrderSaveRepositoryDecorator(
    private val delegate: OrderRepository
) : OrderRepository by delegate {

    override suspend fun save(order: Order, previousOrderVersion: Order?): Order {
        val needSave = if (previousOrderVersion != null) {
            val updatedAt = nowMillis()

            val previous = previousOrderVersion.copy(
                lastUpdateAt = updatedAt
            )
            val current = order.copy(
                lastUpdateAt = updatedAt
            )
            current != previous
        } else {
            true
        }
        return if (needSave) {
            delegate.save(order, previousOrderVersion)
        } else {
            order
        }
    }
}
