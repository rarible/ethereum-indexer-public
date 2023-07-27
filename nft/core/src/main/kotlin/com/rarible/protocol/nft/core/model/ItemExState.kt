package com.rarible.protocol.nft.core.model

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("item_state")
data class ItemExState(
    val isSuspiciousOnOS: Boolean,

    val createdAt: Instant = Instant.now(),
    val lastUpdateAt: Instant = createdAt,
    @Id
    val id: ItemId,
    @Version
    val version: Long? = null
) {
    fun withSuspiciousOnOS(isSuspiciousOnOS: Boolean): ItemExState {
        return copy(isSuspiciousOnOS = isSuspiciousOnOS, lastUpdateAt = Instant.now())
    }

    companion object {
        fun initial(itemId: ItemId): ItemExState {
            return ItemExState(id = itemId, isSuspiciousOnOS = false)
        }
    }
}
