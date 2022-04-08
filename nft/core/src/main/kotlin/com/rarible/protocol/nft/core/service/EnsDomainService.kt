package com.rarible.protocol.nft.core.service

import com.rarible.protocol.nft.core.event.OutgoingEventListener
import com.rarible.protocol.nft.core.model.*
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant

@Service
class EnsDomainService(
    private val actionListeners: List<OutgoingEventListener<ActionEvent>>,
    private val clock: Clock
) {
    suspend fun onGetProperties(itemId: ItemId, properties: ItemProperties) {
        val action =  properties.toAction(itemId)
        actionListeners.forEach { it.onEvent(action) }
    }

    private fun ItemProperties.toAction(itemId: ItemId): ActionEvent {
        val burnAt = if (attributes.isEmpty()) {
            // If attributes is empty we assume that this is expired item, so burn it now
            clock.instant()
        } else {
            val expirationProperty = attributes.firstOrNull { it.key == EXPIRATION_DATE_PROPERTY }?.value
            Instant.parse(expirationProperty)
        }
        return BurnItemActionEvent(
            token = itemId.token,
            tokenId = itemId.tokenId,
            burnAt = burnAt,
        )
    }

    companion object {
        const val EXPIRATION_DATE_PROPERTY = "Expiration Date"
    }
}
