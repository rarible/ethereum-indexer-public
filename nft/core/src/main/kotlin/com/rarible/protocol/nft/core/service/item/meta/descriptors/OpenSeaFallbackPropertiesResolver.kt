package com.rarible.protocol.nft.core.service.item.meta.descriptors

import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.item.meta.ItemPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.ItemResolutionAbortedException
import org.springframework.stereotype.Component
import scalether.domain.Address

// Resolver for collections which are accessible only via OpenSea
// Should be used only for cases when token URI works, but return incorrect data (like redirect to main page)
@Component
class OpenSeaFallbackPropertiesResolver : ItemPropertiesResolver {

    val brokenCollections = setOf(
        Address.apply("0x1906fd9c4ac440561f7197da0a4bd2e88df5fa70") // Aavegotchi, redirecting to main page
    )

    override val name get() = "OpenSeaFallback"

    override suspend fun resolve(itemId: ItemId): ItemProperties? {
        if (brokenCollections.contains(itemId.token)) {
            throw ItemResolutionAbortedException()
        }
        return null
    }

}