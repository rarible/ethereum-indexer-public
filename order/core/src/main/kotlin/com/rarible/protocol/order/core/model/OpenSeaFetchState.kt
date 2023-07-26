package com.rarible.protocol.order.core.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document("open_sea_state")
@Deprecated("Remove in release 1.33")
data class OpenSeaFetchState(
    val listedAfter: Long,
    val cursor: String? = null,
    @Id
    val id: String = ID
) {
    fun withListedAfter(listedAfter: Long): OpenSeaFetchState {
        return copy(listedAfter = listedAfter)
    }

    fun withCursor(cursor: String): OpenSeaFetchState {
        return copy(cursor = cursor)
    }

    companion object {
        const val ID = "open_sea_order_fetch"
    }
}
