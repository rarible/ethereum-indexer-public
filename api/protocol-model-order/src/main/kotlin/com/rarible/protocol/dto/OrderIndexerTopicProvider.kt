package com.rarible.protocol.dto

class OrderIndexerTopicProvider {

    companion object {
        const val VERSION = "v1"

        fun getUpdateTopic(environment: String, blockchain: String): String {
            return "protocol.$environment.$blockchain.indexer.order"
        }

        fun getPriceUpdateTopic(environment: String, blockchain: String): String {
            return "protocol.$environment.$blockchain.indexer.order.price-update"
        }
    }
}
