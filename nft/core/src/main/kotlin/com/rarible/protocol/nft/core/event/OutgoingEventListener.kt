package com.rarible.protocol.nft.core.event

interface OutgoingEventListener<T> {
    suspend fun onEvent(event: T)
}
