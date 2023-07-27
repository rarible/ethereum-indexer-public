package com.rarible.protocol.nft.core.service.item.meta

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.rarible.protocol.nft.core.model.ItemAttribute
import java.time.Instant

fun JsonNode.getText(vararg paths: List<String>): String? {
    for (path in paths) {
        val current = path.fold(this) { node, subPath -> node.path(subPath) }
        if (current.isTextual || current.isNumber) {
            return current.asText()
        }
    }
    return null
}

fun JsonNode.getText(vararg paths: String): String? {
    for (path in paths) {
        val current = this.path(path)
        if (current.isTextual || current.isNumber || current.isBoolean) {
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
    for (attrName in listOf("attributes", "traits", "properties")) {
        val attrPath = path(attrName)
        if (!attrPath.isEmpty && attrPath.isArray) {
            return attrPath.mapNotNull { it.toAttribute(milliTimestamps) }
        }
    }
    return emptyList()
}

fun ObjectNode.getAttribute(key: String) : ItemAttribute? {
    return this.getText(key)?.let { ItemAttribute(key, it) }
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
