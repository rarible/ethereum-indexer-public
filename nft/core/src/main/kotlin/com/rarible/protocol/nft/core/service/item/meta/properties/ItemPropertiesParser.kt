package com.rarible.protocol.nft.core.service.item.meta.properties

import com.fasterxml.jackson.databind.node.ObjectNode
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.item.meta.logMetaLoading

object ItemPropertiesParser {

    fun parse(
        itemId: ItemId,
        httpUrl: String,
        rawProperties: String,
        parser: (ItemId, String) -> ObjectNode = JsonPropertiesParser::parse,
        mapper: (ItemId, ObjectNode) -> ItemProperties = JsonItemPropertiesMapper::map
    ): ItemProperties? {
        return try {
            logMetaLoading(itemId, "parsing properties by URI: $httpUrl")
            if (rawProperties.length > 1_000_000) {
                logMetaLoading(
                    itemId, "suspiciously big item properties ${rawProperties.length} for $httpUrl", warn = true
                )
            }
            val json = parser(itemId, rawProperties)
            val result = mapper(itemId, json)

            if (result.isEmpty()) {
                // TODO ideally we should track it, revisit in meta-pipeline
                logMetaLoading(itemId, "empty meta json received by URI: $httpUrl")
                null
            } else {
                result
            }
        } catch (e: Error) {
            logMetaLoading(itemId, "failed to parse properties by URI: $httpUrl", warn = true)
            throw e
        }
    }
}
