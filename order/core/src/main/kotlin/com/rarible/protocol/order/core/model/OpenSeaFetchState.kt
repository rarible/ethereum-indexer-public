package com.rarible.protocol.order.core.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document("open_sea_state")
data class OpenSeaFetchState(
    val listedAfter: Long,
    @Id
    val id: String = ID
) {
    fun withListedAfter(listedAfter: Long): OpenSeaFetchState {
        return copy(listedAfter = listedAfter)
    }

    companion object {
        const val ID = "open_sea_order_fetch"
    }
}
