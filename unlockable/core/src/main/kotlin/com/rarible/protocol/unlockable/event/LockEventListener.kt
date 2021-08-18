package com.rarible.protocol.unlockable.event

interface LockEventListener {

    suspend fun onEvent(event: LockEvent)

}