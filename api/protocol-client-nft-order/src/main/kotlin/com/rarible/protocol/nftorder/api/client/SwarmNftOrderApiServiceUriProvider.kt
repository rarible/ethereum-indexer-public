package com.rarible.protocol.nftorder.api.client

import java.net.URI

class SwarmNftOrderApiServiceUriProvider(
    private val environment: String
) : NftOrderApiServiceUriProvider {

    override fun getUri(blockchain: String): URI {
        return URI.create(String.format("http://%s-%s-nft-order-api:8080", environment, blockchain))
    }
}