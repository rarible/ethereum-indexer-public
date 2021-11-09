package com.rarible.protocol.dto

class OrderIndexerTopicProvider {

    companion object {
        const val VERSION = "v1"

        fun getOrderUpdateTopic(environment: String, blockchain: String): String {
            return "protocol.$environment.$blockchain.indexer.order"
        }

        fun getAuctionUpdateTopic(environment: String, blockchain: String): String {
            return "protocol.$environment.$blockchain.indexer.auction"
        }

        fun getPriceUpdateTopic(environment: String, blockchain: String): String {
            return "protocol.$environment.$blockchain.indexer.order.price-update"
        }
    }
}
