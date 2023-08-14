package com.rarible.protocol.order.listener.service.event

import com.rarible.core.kafka.RaribleKafkaBatchEventHandler
import com.rarible.protocol.dto.NftOwnershipEventDto
import com.rarible.protocol.order.listener.service.order.OrderBalanceService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class OwnershipConsumerEventHandler(
    private val orderBalanceService: OrderBalanceService
) : RaribleKafkaBatchEventHandler<NftOwnershipEventDto> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handle(event: List<NftOwnershipEventDto>) {
        val start = System.currentTimeMillis()
        coroutineScope {
            event.groupBy { it.ownershipId }.map { group ->
                async {
                    val ownershipEvents = group.value
                    ownershipEvents.forEach { ownershipEvent ->
                        logger.info("Got Ownership event: $ownershipEvent")
                        orderBalanceService.handle(ownershipEvent)
                    }
                }
            }.awaitAll()
        }
        logger.info("Handled group of {} Ownership events ({}ms)", event.size, System.currentTimeMillis() - start)
    }
}
