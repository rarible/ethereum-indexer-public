package com.rarible.protocol.nft.core.service.item.meta.descriptors

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.rarible.protocol.nft.core.model.ItemAttribute
import java.time.Instant

const val ITEM_META_CAPTURE_SPAN_TYPE = "item_meta"
const val TOKEN_META_CAPTURE_SPAN_TYPE = "token_meta"
const val IPFS_CAPTURE_SPAN_TYPE = "ipfs"

fun String?.ifNotBlank() = this?.takeIf { it.isNotBlank() }

fun JsonNode.getText(vararg paths: String): String? {
    for (path in paths) {
        val current = this.path(path)
        if (current.isTextual || current.isNumber) {
            return current.asText()
        }
    }
    return null
}

fun JsonNode.getInt(vararg paths: String): Int? {
    for (path in paths) {
        val current = this.path(path)
        if (current.isInt) {
            return current.asInt()
        }
    }
    return null
}

fun ObjectNode.parseAttributes(milliTimestamps: Boolean = false): List<ItemAttribute> {
    for (attrName in listOf("attributes", "traits")) {
        val attrPath = path(attrName)
        if (!attrPath.isEmpty && attrPath.isArray) {
            return attrPath.mapNotNull { it.toAttribute(milliTimestamps) }
        }
    }
    return emptyList()
}

private fun JsonNode.toAttribute(milliTimestamps: Boolean): ItemAttribute? {
    val key = getText("key", "trait_type") ?: return null
    val valueField = getText("value") ?: return ItemAttribute(key, null, null, null)
    return when {
        getText("display_type") == "date" && valueField.toDoubleOrNull() != null -> {
            val value = if (milliTimestamps) {
                Instant.ofEpochMilli(valueField.toDouble().toLong()).toString()
            } else {
                Instant.ofEpochSecond(valueField.toDouble().toLong()).toString()
            }
            ItemAttribute(key, value, type = "string", format = "date-time")
        }
        else -> {
            val type = getText("type")
            val format = getText("format")
            ItemAttribute(key, valueField, type = type, format = format)
        }
    }
}

