package com.rarible.protocol.nft.core.service.item.meta

import com.fasterxml.jackson.databind.JsonNode
import com.rarible.protocol.nft.core.model.ItemAttribute
import scalether.domain.Address
import java.math.BigInteger
import java.util.*

const val BASE_64_JSON_PREFIX = "data:application/json;base64,"
const val BASE_64_SVG_PREFIX = "data:image/svg+xml;base64,"

fun String.parseTokenId(): Pair<Address, BigInteger> {
    val parts = this.split(":")
    return Pair(Address.apply(parts[0]), parts[1].toBigInteger())
}

fun JsonNode.getText(vararg paths: String): String? {
    for (path in paths) {
        val current = this.path(path)
        if (current.isTextual || current.isNumber) {
            return current.asText()
        }
    }
    return null
}

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
