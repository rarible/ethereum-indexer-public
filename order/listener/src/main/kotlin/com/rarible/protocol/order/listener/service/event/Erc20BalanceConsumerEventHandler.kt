package com.rarible.protocol.order.listener.service.event

import com.rarible.core.kafka.RaribleKafkaEventHandler
import com.rarible.protocol.dto.Erc20BalanceEventDto
import com.rarible.protocol.order.listener.service.order.OrderBalanceService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class Erc20BalanceConsumerEventHandler(
    private val orderBalanceService: OrderBalanceService
) : RaribleKafkaEventHandler<Erc20BalanceEventDto> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handle(event: Erc20BalanceEventDto) {
        logger.info("Got erc20 event: $event")
        orderBalanceService.handle(event)
    }
}
