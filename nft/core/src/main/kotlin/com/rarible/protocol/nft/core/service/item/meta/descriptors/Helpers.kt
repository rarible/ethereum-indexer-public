package com.rarible.protocol.nft.core.service.item.meta.descriptors

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.rarible.protocol.nft.core.model.ItemAttribute
import io.netty.util.internal.ThreadLocalRandom
import java.time.Instant
import java.util.*

const val META_CAPTURE_SPAN_TYPE = "item_meta"

const val BASE_64_JSON_PREFIX = "data:application/json;base64,"
const val BASE_64_SVG_PREFIX = "data:image/svg+xml;base64,"

fun JsonNode.getText(vararg paths: String): String? {
    for (path in paths) {
        val current = this.path(path)
        if (current.isTextual || current.isNumber) {
            return current.asText()
        }
    }
    return null
}

fun ObjectNode.parseAttributes(): List<ItemAttribute> {
    for (attrName in listOf("attributes", "traits")) {
        val attrPath = path(attrName)
        if (!attrPath.isEmpty && attrPath.isArray) {
            return attrPath.mapNotNull { it.toAttribute() }
        }
    }
    return emptyList()
}

private fun JsonNode.toAttribute(): ItemAttribute? {
    val key = getText("key", "trait_type") ?: return null
    val valueField = getText("value") ?: return ItemAttribute(key, null, null, null)
    return when {
        getText("display_type") == "date" && valueField.toDoubleOrNull() != null -> {
            val value = Instant.ofEpochSecond(valueField.toDouble().toLong()).toString()
            ItemAttribute(key, value, type = "string", format = "date-time")
        }
        else -> {
            val type = getText("type")
            val format = getText("format")
            ItemAttribute(key, valueField, type = type, format = format)
        }
    }
}

fun base64MimeToBytes(data: String): ByteArray = Base64.getMimeDecoder().decode(data.toByteArray())

object UserAgentGenerator {
    private const val version1 = "#version1"
    private const val version2 = "#version2"
    private const val version3 = "#version3"

    private val agents = listOf(
        "Mozilla/$version1 (Macintosh; U; PPC Mac OS X; fr-fr) AppleWebKit/$version2 (KHTML, like Gecko) Safari/$version3",
        "Opera/$version1 (X11; Linux x86_64; U; de) Presto/$version2 Version/$version3"
    )

    fun generateUserAgent(): String {
        val template = agents.random()
        return template
            .replace(version1, randomVersion())
            .replace(version2, randomVersion())
            .replace(version3, randomVersion())
    }

    private fun randomVersion(): String {
        return "${ThreadLocalRandom.current().nextInt(1, 30)}.${
            ThreadLocalRandom.current().nextInt(0, 100)
        }.${ThreadLocalRandom.current().nextInt(0, 200)}"
    }
}
