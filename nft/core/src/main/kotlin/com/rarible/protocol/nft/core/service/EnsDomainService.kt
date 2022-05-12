package com.rarible.protocol.nft.core.service

import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.event.OutgoingEventListener
import com.rarible.protocol.nft.core.model.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant

@Component
class EnsDomainService(
    private val actionListeners: List<OutgoingEventListener<ActionEvent>>,
    private val nftIndexerProperties: NftIndexerProperties,
    private val clock: Clock
) {
    private val burnDelay = nftIndexerProperties.action.burnDelay

    suspend fun onGetProperties(itemId: ItemId, properties: ItemProperties) {
        val action =  properties.toAction(itemId)
        actionListeners.forEach { it.onEvent(action) }
    }

    private fun ItemProperties.toAction(itemId: ItemId): ActionEvent {
        val burnAt = if (attributes.isEmpty()) {
            // If attributes is empty we assume that this is expired item, so burn it with delay
            logger.info("Empty EnsDomain properties for ${itemId.decimalStringValue}, burn in $burnDelay")
            clock.instant() + burnDelay
        } else {
            val expirationProperty = getExpirationProperty(this)
            logger.info("EnsDomain item ${itemId.decimalStringValue}, burn on $expirationProperty")
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
        private val logger = LoggerFactory.getLogger(EnsDomainService::class.java)
        const val EXPIRATION_DATE_PROPERTY = "Expiration Date"
    }
}
