package com.rarible.protocol.nft.core.model

import org.springframework.data.annotation.Transient
import scalether.domain.Address
import java.time.Instant

data class UpdateSuspiciousItemsState(
    val statedAt: Instant,
    val assets: List<Asset>,
    val lastUpdatedAt: Instant = Instant.now(),
) {
    fun withLastUpdatedAt(): UpdateSuspiciousItemsState {
        return copy(lastUpdatedAt = Instant.now())
    }

    @get:Transient
    val id: String = STATE_ID

    data class Asset(
        val contract: Address,
        val cursor: String? = null
    )

    companion object {
        const val STATE_ID = "update-suspicious-items"
    }
}
