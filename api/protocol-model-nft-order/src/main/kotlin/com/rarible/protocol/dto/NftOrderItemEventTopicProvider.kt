package com.rarible.protocol.dto

class NftOrderItemEventTopicProvider {

    companion object {
        const val VERSION = "v1"

        fun getTopic(environment: String, blockchain: String): String {
            return "protocol.$environment.$blockchain.nft-order.item"
        }
    }
}
