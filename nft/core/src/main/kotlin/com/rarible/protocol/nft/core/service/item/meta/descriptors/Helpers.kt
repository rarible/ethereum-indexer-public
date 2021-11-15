package com.rarible.protocol.nft.core.service.item.meta

import com.fasterxml.jackson.databind.JsonNode
import com.rarible.protocol.nft.core.model.ItemAttribute
import io.netty.util.internal.ThreadLocalRandom
import scalether.domain.Address
import java.math.BigInteger
import java.time.Instant
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
        this.mapNotNull { it.getText("trait_type")?.let { key -> attribute(key, it) } }
    } else {
        emptyList()
    }
}

fun attribute(key: String, node: JsonNode): ItemAttribute {
    val traitType = TraitType.fromNode(node)
    return ItemAttribute(key, traitType.converter(node), traitType.type, traitType.format)
}

fun isBase64String(data: String): Boolean {
    return data.startsWith(BASE_64_JSON_PREFIX) || data.startsWith(BASE_64_SVG_PREFIX)
}

fun base64MimeToString(data: String): String {
    return String(base64MimeToBytes(data))
}

fun base64MimeToBytes(data: String): ByteArray {
    return Base64.getMimeDecoder().decode(
        data
            .removePrefix(BASE_64_JSON_PREFIX)
            .removePrefix(BASE_64_SVG_PREFIX)
            .toByteArray()
    )
}

enum class TraitType(
    val type: String?,
    val format: String?,
    val predicate: (JsonNode) -> Boolean,
    val converter: (JsonNode) -> String?
) {
    DATE_TIME("string", "date-time",
        { it.getText("display_type") == "date" && it.getText("value")?.toDoubleOrNull() != null },
        {
            val v = it.getText("value") ?: null
            if (v.isNullOrBlank()) {
                null
            } else {
                Instant.ofEpochSecond(v.toDouble().toLong()).toString()
            }
        }),
    DEFAULT(null, null, { true }, { it.getText("value") });

    companion object {
        fun fromNode(node: JsonNode) = values().asSequence()
            .sortedBy { it.ordinal }.find { it.predicate(node) } ?: DEFAULT
    }
}

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
        return "${ThreadLocalRandom.current().nextInt(1, 30)}.${ThreadLocalRandom.current().nextInt(0, 100)}.${ThreadLocalRandom.current().nextInt(0, 200)}"
    }
}
