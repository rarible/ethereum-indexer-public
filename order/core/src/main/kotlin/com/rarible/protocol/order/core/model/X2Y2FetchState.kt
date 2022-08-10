package com.rarible.protocol.order.core.model

import java.time.Instant
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.MongoId

@Document("x2y2_state")
data class X2Y2FetchState(
    @MongoId
    val id: String = ID,
    val cursor: String? = null,
    @LastModifiedDate
    val lastUpdatedAt: Instant? = null,
    val lastError: String? = null
) {
    companion object {
        fun withCursor(cursor: String): X2Y2FetchState {
            return X2Y2FetchState(cursor = cursor)
        }

        const val ID = "x2y2_order_fetch"
    }
}
