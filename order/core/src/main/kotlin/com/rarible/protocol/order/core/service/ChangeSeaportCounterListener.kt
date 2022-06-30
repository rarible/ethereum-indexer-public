package com.rarible.protocol.order.core.service

import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.block.ChangeNonceListener
import kotlinx.coroutines.flow.collect
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class ChangeSeaportCounterListener(
    private val orderRepository: OrderRepository,
    private val orderUpdateService: OrderUpdateService
) : ChangeNonceListener {

    override suspend fun onNewMakerNonce(maker: Address, newNonce: Long) {
        require(newNonce > 0) {
            "Maker $maker nonce is less then zero $newNonce"
        }
        logger.info("New Seaport counter $newNonce detected for maker $maker")
        orderRepository
            .findNotCanceledByMakerAndByCounter(maker = maker,  counter = newNonce - 1)
            .collect { hash ->
                orderUpdateService.update(hash)
            }
    }

    private companion object {
        val logger: Logger = LoggerFactory.getLogger(ChangeOpenSeaNonceListener::class.java)
    }
}