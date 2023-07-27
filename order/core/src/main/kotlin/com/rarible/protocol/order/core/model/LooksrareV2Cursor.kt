package com.rarible.protocol.order.core.model

import java.time.Instant

data class LooksrareV2Cursor(
    val createdAfter: Instant,
    val nextId: String? = null,
    val maxSeenCreated: Instant? = null
) {
    companion object {
        @Deprecated("Remove after migration")
        fun parser(cursor: String): LooksrareV2Cursor {
            val parts = cursor.split(":")
            return LooksrareV2Cursor(
                createdAfter = Instant.ofEpochSecond(parts[0].toLong()),
                nextId = parts.getOrNull(1),
            )
        }

        fun default() = LooksrareV2Cursor(Instant.now())
    }
}
