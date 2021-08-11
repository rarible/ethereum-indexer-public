package com.rarible.protocol.nftorder.core.event

interface OwnershipEventListener {

    suspend fun onEvent(event: OwnershipEvent)

}