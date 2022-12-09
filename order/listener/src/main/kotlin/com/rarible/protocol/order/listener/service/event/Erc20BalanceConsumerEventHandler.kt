package com.rarible.protocol.order.listener.service.event

import com.rarible.core.daemon.sequential.ConsumerEventHandler
import com.rarible.protocol.dto.Erc20BalanceEventDto
import com.rarible.protocol.order.listener.service.order.OrderBalanceService
import kotlinx.coroutines.time.delay
import org.slf4j.LoggerFactory
import java.time.Duration

class Erc20BalanceConsumerEventHandler(
    private val orderBalanceService: OrderBalanceService
) : ConsumerEventHandler<Erc20BalanceEventDto> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handle(event: Erc20BalanceEventDto) {
        logger.info("Got erc20 event: $event")
        delay(Duration.ofSeconds(10))
        orderBalanceService.handle(event)
    }
}
