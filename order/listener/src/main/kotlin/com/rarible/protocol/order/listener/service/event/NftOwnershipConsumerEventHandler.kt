package com.rarible.protocol.order.listener.service.event

import com.rarible.core.daemon.sequential.ConsumerEventHandler
import com.rarible.protocol.dto.NftOwnershipEventDto
import com.rarible.protocol.order.listener.service.order.OrderBalanceService
import kotlinx.coroutines.time.delay
import org.slf4j.LoggerFactory
import java.time.Duration

class NftOwnershipConsumerEventHandler(
    private val orderBalanceService: OrderBalanceService
) : ConsumerEventHandler<NftOwnershipEventDto> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handle(event: NftOwnershipEventDto) {
        logger.info("Got nft ownership event: $event")
        orderBalanceService.handle(event)
    }
}
