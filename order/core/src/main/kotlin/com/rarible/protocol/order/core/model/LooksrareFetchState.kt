package com.rarible.protocol.order.core.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("looksrare_state")
data class LooksrareFetchState(
    val cursor: String? = null,
    val listedAfter: Instant? = null,

    @Id
    val id: String = ID
) {

    fun withCursor(cursor: String): LooksrareFetchState {
        return copy(cursor = cursor)
    }

    companion object {
        const val ID = "looksrare_order_fetch"
    }
}