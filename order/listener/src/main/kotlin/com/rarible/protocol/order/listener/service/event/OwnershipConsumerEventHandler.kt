package com.rarible.protocol.order.listener.service.event

import com.rarible.core.kafka.RaribleKafkaEventHandler
import com.rarible.protocol.dto.NftOwnershipEventDto
import com.rarible.protocol.order.listener.service.order.OrderBalanceService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class OwnershipConsumerEventHandler(
    private val orderBalanceService: OrderBalanceService
) : RaribleKafkaEventHandler<NftOwnershipEventDto> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handle(event: NftOwnershipEventDto) {
        logger.info("Got nft ownership event: $event")
        orderBalanceService.handle(event)
    }
}
