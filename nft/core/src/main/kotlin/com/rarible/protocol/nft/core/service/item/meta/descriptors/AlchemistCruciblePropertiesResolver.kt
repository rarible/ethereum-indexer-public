package com.rarible.protocol.nft.core.service.item.meta.descriptors

import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.UrlService
import com.rarible.protocol.nft.core.service.item.meta.ItemResolutionAbortedException
import com.rarible.protocol.nft.core.service.item.meta.properties.ItemPropertiesParser
import com.rarible.protocol.nft.core.service.item.meta.properties.RawPropertiesProvider
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class AlchemistCruciblePropertiesResolver(
    private val urlService: UrlService,
    private val rawPropertiesProvider: RawPropertiesProvider
) : ItemPropertiesResolver {

    override val name = "AlchemistCrucibleV1"

    override suspend fun resolve(itemId: ItemId): ItemProperties? {
        if (itemId.token != ALCHEMIST_CRUCIBLE_V1_ADDRESS) {
            return null
        }
        val httpUrl = "https://crucible.wtf/nft-meta/${itemId.tokenId.value}?network=1"

        val resource = urlService.parseUrl(httpUrl, itemId.toString()) ?: return null
        val rawProperties = rawPropertiesProvider.getContent(itemId, resource) ?: return null

        return runCatching {
            ItemPropertiesParser.parse(
                itemId = itemId,
                httpUrl = urlService.resolveInternalHttpUrl(resource),
                rawProperties = rawProperties
            )
        }.getOrElse {
            throw ItemResolutionAbortedException()
        }
    }

    companion object {
        val ALCHEMIST_CRUCIBLE_V1_ADDRESS = Address.apply("0x54e0395cfb4f39bef66dbcd5bd93cca4e9273d56")
    }
}
