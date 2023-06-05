package com.rarible.protocol.order.core.model

import java.time.Instant

data class LooksrareV2Cursor(
    val createdAfter: Instant,
    val nextId: String? = null,
) {
    override fun toString(): String {
        return buildString {
            append(createdAfter.epochSecond)
            if (nextId != null) {
                append(":")
                append(nextId)
            }
        }
    }

    companion object {
        fun parser(cursor: String): LooksrareV2Cursor {
            val parts = cursor.split(":")
            return LooksrareV2Cursor(
                createdAfter = Instant.ofEpochSecond(parts[0].toLong()),
                nextId = parts.getOrNull(1)
            )
        }
    }
}