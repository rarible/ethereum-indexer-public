package com.rarible.protocol.nftorder.listener.handler

import com.rarible.protocol.dto.NftItemDeleteEventDto
import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.dto.NftItemUpdateEventDto
import com.rarible.protocol.nftorder.core.model.ItemId
import com.rarible.protocol.nftorder.listener.service.ItemEventService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ItemEventHandler(
    private val itemEventService: ItemEventService
) : AbstractEventHandler<NftItemEventDto>() {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handleSafely(event: NftItemEventDto) {
        logger.debug("Received item event: type=${event::class.java.simpleName}")
        when (event) {
            is NftItemUpdateEventDto -> {
                itemEventService.onItemUpdated(event.item)
            }
            is NftItemDeleteEventDto -> {
                val itemId = ItemId.parseId(event.itemId)
                itemEventService.onItemDeleted(itemId)
            }
            else -> {
                logger.warn("Unsupported NftItem event type: {}", event)
            }
        }
    }
}