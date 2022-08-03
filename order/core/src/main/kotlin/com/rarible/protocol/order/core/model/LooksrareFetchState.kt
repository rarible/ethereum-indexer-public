package com.rarible.protocol.order.core.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("looksrare_state")
data class LooksrareFetchState(
    val listedAfter: Instant,
    @Id
    val id: String = ID
) {
    fun withListedAfter(listedAfter: Instant): LooksrareFetchState {
        return copy(listedAfter = listedAfter)
    }

    companion object {
        const val ID = "looksrare_order_fetch"
    }
}