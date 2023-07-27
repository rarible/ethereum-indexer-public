package com.rarible.protocol.order.listener.service.event

import com.rarible.core.kafka.RaribleKafkaEventHandler
import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.dto.NftItemUpdateEventDto
import com.rarible.protocol.order.listener.service.order.OrderItemService
import org.springframework.stereotype.Component

@Component
class ItemConsumerEventHandler(
    private val orderItemService: OrderItemService
) : RaribleKafkaEventHandler<NftItemEventDto> {

    override suspend fun handle(event: NftItemEventDto) {
        when (event) {
            is NftItemUpdateEventDto -> orderItemService.onItemChanged(event)
            else -> {}
        }
    }
}
