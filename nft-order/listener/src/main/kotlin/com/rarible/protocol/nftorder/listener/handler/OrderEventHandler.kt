package com.rarible.protocol.nftorder.listener.handler

import com.rarible.protocol.dto.OrderEventDto
import com.rarible.protocol.dto.OrderUpdateEventDto
import com.rarible.protocol.nftorder.listener.service.OrderEventService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class OrderEventHandler(
    private val orderEventService: OrderEventService
) : AbstractEventHandler<OrderEventDto>() {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handleSafely(event: OrderEventDto) {
        logger.debug("Received Order event: type=${event::class.java.simpleName}")
        when (event) {
            is OrderUpdateEventDto -> {
                orderEventService.updateOrder(event.order)
            }
            else -> {
                logger.warn("Unsupported Order event type: {}", event)
            }
        }
    }

}