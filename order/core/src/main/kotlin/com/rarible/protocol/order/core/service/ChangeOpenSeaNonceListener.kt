package com.rarible.protocol.order.core.service

import com.rarible.protocol.order.core.repository.order.OrderRepository
import kotlinx.coroutines.flow.collect
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class ChangeOpenSeaNonceListener(
    private val orderRepository: OrderRepository,
    private val orderUpdateService: OrderUpdateService
) {
    suspend fun onNewMakerNonce(maker: Address, newNonce: Long) {
        require(newNonce > 0) {
            "Maker $maker nonce is less then zero $newNonce"
        }
        orderRepository
            .findOpenSeaHashesByMakerAndByNonce(maker, fromIncluding = newNonce - 1,  toExcluding = newNonce)
            .collect { hash -> orderUpdateService.update(hash) }
    }
}