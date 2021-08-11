package com.rarible.protocol.nftorder.listener.handler

import com.rarible.protocol.dto.UnlockableEventDto
import com.rarible.protocol.nftorder.core.model.ItemId
import com.rarible.protocol.nftorder.listener.service.ItemEventService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class UnlockableEventHandler(
    private val itemEventService: ItemEventService
) : AbstractEventHandler<UnlockableEventDto>() {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handleSafely(event: UnlockableEventDto) {
        logger.debug("Received Unlockable event : [{}]", event)
        when (event.type) {
            UnlockableEventDto.Type.LOCK_CREATED -> {
                val itemId = ItemId.parseId(event.itemId)
                itemEventService.onLockCreated(itemId)
            }
            UnlockableEventDto.Type.LOCK_UNLOCKED -> {
                // No updates required, we already know this item is Unlockable by LOCK_CREATED event
            }
        }
    }
}