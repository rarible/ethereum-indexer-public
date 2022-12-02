package com.rarible.protocol.order.core.model

import com.rarible.ethereum.domain.Blockchain

object EntityEventListeners {
    fun orderHistoryListenerId(env: String, blockchain: Blockchain): String =
        "${prefix(env, blockchain)}.order-history.listener"

    private fun prefix(env: String, blockchain: Blockchain): String = "$env.protocol.${blockchain.value}.order"
}
