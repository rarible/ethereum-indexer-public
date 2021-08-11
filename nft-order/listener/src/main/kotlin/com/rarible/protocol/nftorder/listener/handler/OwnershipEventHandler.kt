package com.rarible.protocol.nftorder.listener.handler

import com.rarible.protocol.dto.NftOwnershipDeleteEventDto
import com.rarible.protocol.dto.NftOwnershipEventDto
import com.rarible.protocol.dto.NftOwnershipUpdateEventDto
import com.rarible.protocol.nftorder.listener.service.OwnershipEventService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class OwnershipEventHandler(
    private val ownershipEventService: OwnershipEventService
) : AbstractEventHandler<NftOwnershipEventDto>() {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handleSafely(event: NftOwnershipEventDto) {
        logger.debug("Received NftOwnership event: type=${event::class.java.simpleName}")
        when (event) {
            is NftOwnershipUpdateEventDto -> {
                ownershipEventService.onOwnershipUpdated(event.ownership)
            }
            is NftOwnershipDeleteEventDto -> {
                ownershipEventService.onOwnershipDeleted(event.ownership)
            }
            else -> {
                logger.warn("Unsupported NftOwnership event type: {}", event)
            }
        }
    }
}