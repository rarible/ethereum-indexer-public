package com.rarible.protocol.dto

class Erc20BalanceEventTopicProvider {

    companion object {
        val VERSION = "1"

        fun getTopic(environment: String, blockchain: String): String {
            return "protocol.$environment.$blockchain.indexer.erc20.balance"
        }
    }
}
