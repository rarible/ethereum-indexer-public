package com.rarible.protocol.nft.api.service.item.meta

import com.fasterxml.jackson.databind.JsonNode
import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.service.item.meta.getText
import java.util.*

fun JsonNode.toProperties(): List<ItemAttribute> {
    return if (this.isArray) {
        this.mapNotNull { it.getText("trait_type")?.let { key -> ItemAttribute(key, it.getText("value")) } }
    } else {
        emptyList()
    }
}

fun base64MimeToString(data: String): String {
    return String(base64MimeToBytes(data))
}

fun base64MimeToBytes(data: String): ByteArray {
    return Base64.getMimeDecoder().decode(data
        .removePrefix("data:application/json;base64,")
        .removePrefix("data:image/svg+xml;base64,")
        .toByteArray())
}
