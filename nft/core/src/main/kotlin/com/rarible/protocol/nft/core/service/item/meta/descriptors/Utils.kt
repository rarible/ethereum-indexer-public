package com.rarible.protocol.nft.api.service.item.meta

import com.fasterxml.jackson.databind.JsonNode
import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.service.item.meta.getText

fun JsonNode.toProperties(): List<ItemAttribute> {
    return if (this.isArray) {
        this.mapNotNull { it.getText("trait_type")?.let { key -> ItemAttribute(key, it.getText("value")) } }
    } else {
        emptyList()
    }
}
