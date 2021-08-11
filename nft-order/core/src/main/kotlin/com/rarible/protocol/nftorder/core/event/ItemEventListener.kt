package com.rarible.protocol.nftorder.core.event

interface ItemEventListener {

    suspend fun onEvent(event: ItemEvent)

}