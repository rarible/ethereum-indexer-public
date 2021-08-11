package com.rarible.protocol.dto

class NftItemEventTopicProvider {

    companion object {
        const val VERSION = "v1"

        fun getTopic(environment: String, blockchain: String): String {
            return "protocol.$environment.$blockchain.indexer.nft.item"
        }
    }
}
