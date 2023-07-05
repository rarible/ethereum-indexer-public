package com.rarible.protocol.order.core.model

import com.rarible.ethereum.domain.Blockchain

object EntityEventListeners {
    fun orderHistoryListenerId(blockchain: Blockchain): String =
        "${prefix(blockchain)}.order-history.listener"

    private fun prefix(blockchain: Blockchain): String = "protocol.${blockchain.value}.order"
}
