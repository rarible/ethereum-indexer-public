package com.rarible.protocol.order.core.service

import com.rarible.protocol.order.core.event.OrderVersionListener
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.repository.order.OrderVersionRepository
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.stereotype.Component

@Component
class OrderVersionService(
    private val orderReduceService: OrderReduceService,
    private val orderVersionRepository: OrderVersionRepository,
    private val orderVersionListener: OrderVersionListener
) {
    suspend fun addOrderVersion(orderVersion: OrderVersion): Order {
        orderVersionRepository.save(orderVersion).awaitFirst()
        val order = orderReduceService.update(orderHash = orderVersion.hash).awaitSingle()
        orderVersionListener.onOrderVersion(orderVersion)
        return order
    }
}