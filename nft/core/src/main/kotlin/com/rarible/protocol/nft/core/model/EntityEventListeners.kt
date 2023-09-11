package com.rarible.protocol.nft.core.model

import com.rarible.ethereum.domain.Blockchain

object EntityEventListeners {

    fun itemHistoryListenerId(blockchain: Blockchain): String =
        "${prefix(blockchain)}.item.history.listener"

    fun ownershipHistoryListenerId(blockchain: Blockchain): String =
        "${prefix(blockchain)}.ownership.history.listener"

    fun tokenHistoryListenerId(blockchain: Blockchain): String =
        "${prefix(blockchain)}.token.history.listener"

    fun tokenRevealListenerId(blockchain: Blockchain): String =
        "${prefix(blockchain)}.token.reveal.listener"

    private fun prefix(blockchain: Blockchain): String = "protocol.${blockchain.value}.nft"
}
