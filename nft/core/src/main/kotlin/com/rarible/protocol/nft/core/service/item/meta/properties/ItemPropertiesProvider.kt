package com.rarible.protocol.nft.core.service.item.meta.properties

import com.fasterxml.jackson.databind.node.ObjectNode
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.item.meta.logMetaLoading

object ItemPropertiesProvider {

    fun provide(
        itemId: ItemId,
        httpUrl: String,
        rawProperties: String,
        parser: (ItemId, String) -> ObjectNode? = JsonPropertiesParser::parse,
        mapper: (ItemId, ObjectNode) -> ItemProperties? = JsonPropertiesMapper::map
    ): ItemProperties? {
        return try {
            logMetaLoading(itemId, "parsing properties by URI: $httpUrl")
            if (rawProperties.length > 1_000_000) {
                logMetaLoading(itemId, "suspiciously big item properties ${rawProperties.length} for $httpUrl", warn = true)
            }
            val json = parser(itemId, rawProperties)
            json?.let { mapper(itemId, json) }
        } catch (e: Error) {
            logMetaLoading(itemId, "failed to parse properties by URI: $httpUrl", warn = true)
            null
        }
    }
}
