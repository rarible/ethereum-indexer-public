package com.rarible.protocol.order.core.service

import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.order.OrderFilter
import com.rarible.protocol.order.core.repository.order.OrderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.springframework.stereotype.Component

@Component
class OrderRepositoryService(
    private val orderRepository: OrderRepository
) {
    suspend fun search(filter: OrderFilter, batchSize: Int): Flow<List<Order>> = flow {
        var continuation: String? = null
        do {
            val orders = orderRepository.search(filter.toQuery(continuation, batchSize))

            continuation = if (orders.isNotEmpty()) {
                emit(orders)
                filter.toContinuation(orders.last())
            } else {
                null
            }
        } while (orders.size == batchSize)
     }
}
