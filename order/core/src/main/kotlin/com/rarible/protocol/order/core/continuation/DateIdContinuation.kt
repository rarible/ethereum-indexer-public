package com.rarible.protocol.order.core.continuation

import java.time.Instant

data class DateIdContinuation(
    val date: Instant,
    val id: String
) : Continuation {

    override fun toString(): String {
        return "${date.toEpochMilli()}_${id}"
    }

    companion object {
        fun parse(str: String?): DateIdContinuation? {
            val pair = Continuation.splitBy(str, "_") ?: return null
            val timestamp = pair.first
            val id = pair.second
            return DateIdContinuation(Instant.ofEpochMilli(timestamp.toLong()), id)
        }
    }
}
