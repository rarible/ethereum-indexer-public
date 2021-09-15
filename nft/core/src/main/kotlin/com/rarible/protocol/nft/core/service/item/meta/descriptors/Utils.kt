package com.rarible.protocol.nft.api.service.item.meta

import com.fasterxml.jackson.databind.JsonNode
import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.service.item.meta.getText
import java.util.*

const val BASE_64_JSON_PREFIX = "data:application/json;base64,"
const val BASE_64_SVG_PREFIX = "data:image/svg+xml;base64,"

fun JsonNode.toProperties(): List<ItemAttribute> {
    return if (this.isArray) {
        this.mapNotNull { it.getText("trait_type")?.let { key -> ItemAttribute(key, it.getText("value")) } }
    } else {
        emptyList()
    }
}

fun isBase64String(data: String): Boolean {
    return data.startsWith(BASE_64_JSON_PREFIX) || data.startsWith(BASE_64_SVG_PREFIX)
}

fun base64MimeToString(data: String): String {
    return String(base64MimeToBytes(data))
}

fun base64MimeToBytes(data: String): ByteArray {
    return Base64.getMimeDecoder().decode(data
        .removePrefix(BASE_64_JSON_PREFIX)
        .removePrefix(BASE_64_SVG_PREFIX)
        .toByteArray())
}
