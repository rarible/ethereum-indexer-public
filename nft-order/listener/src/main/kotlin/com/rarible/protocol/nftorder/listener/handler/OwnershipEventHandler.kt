package com.rarible.protocol.nftorder.listener.handler

import com.rarible.core.common.nowMillis
import com.rarible.protocol.dto.NftOwnershipDeleteEventDto
import com.rarible.protocol.dto.NftOwnershipEventDto
import com.rarible.protocol.dto.NftOwnershipUpdateEventDto
import com.rarible.protocol.nftorder.core.util.spent
import com.rarible.protocol.nftorder.listener.service.OwnershipEventService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class OwnershipEventHandler(
    private val ownershipEventService: OwnershipEventService
) : AbstractEventHandler<NftOwnershipEventDto>() {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handleSafely(event: NftOwnershipEventDto) {
        val now = nowMillis()
        logger.debug("Received NftOwnership event: type={}", event::class.java.simpleName)
        when (event) {
            is NftOwnershipUpdateEventDto -> {
                ownershipEventService.onOwnershipUpdated(event.ownership)
                logger.info("Ownership [{}] updated ({}ms)", event.ownershipId, spent(now))
            }
            is NftOwnershipDeleteEventDto -> {
                ownershipEventService.onOwnershipDeleted(event.ownership)
                logger.info("Ownership [{}] deleted ({}ms)", event.ownershipId, spent(now))
            }
            else -> {
                logger.warn("Unsupported NftOwnership event type: {}", event)
            }
        }
    }
}