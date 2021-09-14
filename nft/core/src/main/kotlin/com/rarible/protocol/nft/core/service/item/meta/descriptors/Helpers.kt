package com.rarible.protocol.nft.core.service.item.meta

import com.fasterxml.jackson.databind.JsonNode
import scalether.domain.Address
import java.math.BigInteger

fun String.parseTokenId(): Pair<Address, BigInteger> {
    val parts = this.split(":")
    return Pair(Address.apply(parts[0]), parts[1].toBigInteger())
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

