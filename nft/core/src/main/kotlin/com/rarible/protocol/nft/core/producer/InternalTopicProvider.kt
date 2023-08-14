package com.rarible.protocol.nft.core.producer

object InternalTopicProvider {
    const val VERSION = "v1"

    fun getItemActionTopic(environment: String, blockchain: String): String {
        return "protocol.$environment.$blockchain.item.internal.action"
    }
}
