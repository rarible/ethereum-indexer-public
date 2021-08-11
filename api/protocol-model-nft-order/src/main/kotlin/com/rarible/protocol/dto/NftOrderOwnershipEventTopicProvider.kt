package com.rarible.protocol.dto

class NftOrderOwnershipEventTopicProvider {

    companion object {
        const val VERSION = "v1"

        fun getTopic(environment: String, blockchain: String): String {
            return "protocol.$environment.$blockchain.nft-order.ownership"
        }
    }
}
