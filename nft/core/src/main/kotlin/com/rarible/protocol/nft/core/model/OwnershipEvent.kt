package com.rarible.protocol.nft.core.model

import java.time.Instant

data class OwnershipEvent(
    val id: String,
    val entityId: String,
    val timestamp: Instant,
    val subEvents: List<OwnershipSubEvent>
)

sealed class OwnershipSubEvent {
    abstract val type: SubOwnershipEventType
}

data class UpdateOwnershipEvent(
    val ownership: Ownership
) : OwnershipSubEvent() {
    override val type: SubOwnershipEventType = SubOwnershipEventType.OWNERSHIP_UPDATED
}

data class DeleteOwnershipEvent(
    val ownershipId: String
) : OwnershipSubEvent() {
    override val type: SubOwnershipEventType = SubOwnershipEventType.OWNERSHIP_DELETED
}

enum class SubOwnershipEventType {
    OWNERSHIP_UPDATED,
    OWNERSHIP_DELETED,
}