package com.rarible.protocol.unlockable.event

data class LockEvent(
    val id: String,
    val itemId: String,
    val type: LockEventType
)

enum class LockEventType {
    LOCK_CREATED, LOCK_UNLOCKED
}

