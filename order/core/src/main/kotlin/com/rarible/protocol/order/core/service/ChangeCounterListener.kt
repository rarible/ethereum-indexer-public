package com.rarible.protocol.order.core.service

import com.rarible.core.common.EventTimeMarks
import com.rarible.core.common.asyncBatchHandle
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.repository.order.OrderRepository
import kotlinx.coroutines.flow.toList
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import scalether.domain.Address
import java.math.BigInteger
import java.time.Instant

@Component
class ChangeCounterListener(
    private val orderRepository: OrderRepository,
    private val orderUpdateService: OrderUpdateService,
    private val properties: OrderIndexerProperties,
) {

    suspend fun onNewMakerNonce(
        platform: Platform,
        maker: Address,
        newNonce: BigInteger,
        ts: Instant,
        eventTimeMarks: EventTimeMarks
    ) {
        require(newNonce > BigInteger.ZERO) {
            "Maker $maker nonce is less then zero $newNonce"
        }
        logger.info("New $platform counter $newNonce detected for maker $maker")
        // We hava the index for this, and we only get hashes field from this index,
        // so we can just get them all, without much performance issue
        orderRepository
            .findNotCanceledByMakerAndCounterLtThen(platform, maker, newNonce)
            .toList()
            .asyncBatchHandle(properties.batchHandle.countChangeBatchSize) {
                orderUpdateService.update(it, eventTimeMarks)
            }
    }

    private companion object {

        val logger: Logger = LoggerFactory.getLogger(ChangeCounterListener::class.java)
    }
}
