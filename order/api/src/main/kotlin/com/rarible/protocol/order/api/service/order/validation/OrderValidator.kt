package com.rarible.protocol.order.api.service.order.validation

import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderVersion
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Service

@Service
class OrderValidator(
    private val orderVersionValidators: List<OrderVersionValidator>,
    private val orderPatchValidators: List<OrderPatchValidator>
) {
    suspend fun validate(orderVersion: OrderVersion) = coroutineScope<Unit> {
        orderVersionValidators.map { async { it.validate(orderVersion) } }.awaitAll()
    }

    suspend fun validate(order: Order, update: OrderVersion) = coroutineScope<Unit> {
        orderPatchValidators.map { async { it.validate(order, update) } }.awaitAll()
    }
}
