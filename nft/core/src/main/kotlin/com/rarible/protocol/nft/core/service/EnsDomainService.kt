package com.rarible.protocol.nft.core.service

import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.event.OutgoingEventListener
import com.rarible.protocol.nft.core.model.*
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant

@Component
class EnsDomainService(
    private val actionListeners: List<OutgoingEventListener<ActionEvent>>,
    private val nftIndexerProperties: NftIndexerProperties,
    private val clock: Clock
) {
    suspend fun onGetProperties(itemId: ItemId, properties: ItemProperties) {
        val action =  properties.toAction(itemId)
        actionListeners.forEach { it.onEvent(action) }
    }

    private fun ItemProperties.toAction(itemId: ItemId): ActionEvent {
        val burnAt = if (attributes.isEmpty()) {
            // If attributes is empty we assume that this is expired item, so burn it with delay
            clock.instant() + nftIndexerProperties.action.burnDelay
        } else {
            val expirationProperty = getExpirationProperty(this)
            Instant.parse(expirationProperty)
        }
        return BurnItemActionEvent(
            token = itemId.token,
            tokenId = itemId.tokenId,
            burnAt = burnAt,
        )
    }

    fun getExpirationProperty(properties: ItemProperties): String? {
        return  properties.attributes.firstOrNull { it.key == EXPIRATION_DATE_PROPERTY }?.value
    }

    companion object {
        const val EXPIRATION_DATE_PROPERTY = "Expiration Date"
    }
}
