package com.rarible.protocol.nft.core.model

object EntityEventListeners {
    fun itemHistoryListenerId(env: String, blockchain: String): String {
        return "${prefix(env, blockchain)}.item.history.listener"
    }

    fun ownershipHistoryListenerId(env: String, blockchain: String): String {
        return "${prefix(env, blockchain)}.ownership.history.listener"
    }

    fun tokenHistoryListenerId(env: String, blockchain: String): String {
        return "${prefix(env, blockchain)}.token.history.listener"
    }

    private fun prefix(env: String, blockchain: String): String = "$env.protocol.$blockchain.nft"
}
