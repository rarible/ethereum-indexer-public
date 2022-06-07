package com.rarible.protocol.nft.core.service.item.meta.descriptors

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.item.meta.ItemPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.ItemResolutionAbortedException
import com.rarible.protocol.nft.core.service.item.meta.logMetaLoading
import com.rarible.protocol.nft.core.service.item.meta.properties.JsonPropertiesMapper
import com.rarible.protocol.nft.core.service.item.meta.properties.JsonPropertiesParser
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
@CaptureSpan(type = ITEM_META_CAPTURE_SPAN_TYPE)
class AlchemistCruciblePropertiesResolver(
    private val propertiesHttpLoader: PropertiesHttpLoader
) : ItemPropertiesResolver {

    override val name = "AlchemistCrucibleV1"

    override suspend fun resolve(itemId: ItemId): ItemProperties? {
        if (itemId.token != ALCHEMIST_CRUCIBLE_V1_ADDRESS) {
            return null
        }
        val httpUrl = "https://crucible.wtf/nft-meta/${itemId.tokenId.value}?network=1"
        val propertiesString = propertiesHttpLoader.getByUrl(itemId, httpUrl) ?: return null

        val result = try {
            logMetaLoading(itemId, "parsing properties by URI: $httpUrl")
            val json = JsonPropertiesParser.parse(itemId, propertiesString)
            json?.let { JsonPropertiesMapper.map(itemId, json) }
        } catch (e: Error) {
            logMetaLoading(itemId, "failed to parse properties by URI: $httpUrl", warn = true)
            null
        }

        return result ?: throw ItemResolutionAbortedException()
    }

    companion object {

        val ALCHEMIST_CRUCIBLE_V1_ADDRESS = Address.apply("0x54e0395cfb4f39bef66dbcd5bd93cca4e9273d56")
    }

}