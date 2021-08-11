package com.rarible.protocol.nft.api.service.item.meta

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.rarible.protocol.nft.core.model.ItemAttribute
import scalether.domain.Address
import java.math.BigInteger

fun String.parseTokenId(): Pair<Address, BigInteger> {
    val parts = this.split(":")
    return Pair(Address.apply(parts[0]), parts[1].toBigInteger())
}

fun ArrayNode.addNode(key: String, value: String): ArrayNode {
    this.add(JsonNodeFactory.instance.objectNode().put("key", key).put("value", value))
    return this
}

fun JsonNode.getText(vararg paths: String): String? {
    for (path in paths) {
        val current = this.path(path)
        if (current.isTextual) {
            return current.asText()
        }
    }
    return null
}

fun convertObjectAttributes(attrs: ObjectNode): List<ItemAttribute> {
    return attrs.fields().asSequence()
        .mapNotNull { e ->
            ItemAttribute(e.key, e.value.asText())
        }
        .toList()
}

fun convertArrayAttributes(attrs: ArrayNode): List<ItemAttribute> {
    return attrs.mapNotNull {
        val key = it.getText("key", "trait_type")
        if (key != null)
            ItemAttribute(key, it.getText("value"))
        else
            null
    }
}

