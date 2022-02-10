package com.rarible.protocol.nft.core.model

import com.rarible.ethereum.domain.Blockchain

object EntityEventListeners {
    fun itemHistoryListenerId(env: String, blockchain: Blockchain): String {
        return "${prefix(env, blockchain)}.item.history.listener"
    }

    fun tokenHistoryListenerId(env: String, blockchain: Blockchain): String {
        return "${prefix(env, blockchain)}.token.history.listener"
    }

    private fun prefix(env: String, blockchain: Blockchain): String = "$env.protocol.${blockchain.value}.nft"
}
