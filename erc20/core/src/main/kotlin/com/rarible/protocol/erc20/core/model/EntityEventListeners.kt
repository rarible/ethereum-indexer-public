package com.rarible.protocol.erc20.core.model

import com.rarible.ethereum.domain.Blockchain

object EntityEventListeners {

    fun erc20HistoryListenerId(env: String, blockchain: Blockchain): String =
        "${prefix(env, blockchain)}.token.history.listener"

    private fun prefix(env: String, blockchain: Blockchain): String = "$env.protocol.${blockchain.value}.erc20"
}
