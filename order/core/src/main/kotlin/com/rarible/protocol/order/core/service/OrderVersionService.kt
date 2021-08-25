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
    @Throws(OrderReduceService.OrderUpdateError::class)
    suspend fun addOrderVersion(orderVersion: OrderVersion): Order {
        // Try to update the Order state with the new [orderVersion]. Do not yet add the version to the OrderVersionRepository.
        // If the [orderVersion] leads to an invalid update, this function will fail at [orderValidator.validate].
        val order = orderReduceService.update(orderHash = orderVersion.hash, newOrderVersion = orderVersion).awaitSingle()
        /*
        TODO: this is not 100% correct to insert the order version now,
              because there might have been other new versions added,
              making our [orderVersion] to be not valid anymore.
              Probably we need transactional insertion here.
         */
        orderVersionRepository.save(orderVersion).awaitFirst()
        orderVersionListener.onOrderVersion(orderVersion)
        return order
    }
}