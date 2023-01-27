package com.rarible.protocol.order.core.service

import com.rarible.protocol.dto.blockchainEventMark
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.repository.order.OrderRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import scalether.domain.Address
import java.time.Instant

@Component
class ChangeCounterListener(
    private val orderRepository: OrderRepository,
    private val orderUpdateService: OrderUpdateService
) {

    suspend fun onNewMakerNonce(platform: Platform, maker: Address, newNonce: Long, ts: Instant) {
        require(newNonce > 0) {
            "Maker $maker nonce is less then zero $newNonce"
        }
        logger.info("New $platform counter $newNonce detected for maker $maker")
        orderRepository
            .findNotCanceledByMakerAndCounterLtThen(platform, maker, newNonce)
            .collect { hash ->
                orderUpdateService.update(hash, blockchainEventMark("indexer-in_order", ts))
            }
    }

    private companion object {

        val logger: Logger = LoggerFactory.getLogger(ChangeCounterListener::class.java)
    }
}