package com.rarible.protocol.nft.api.client

import java.net.URI

class SwarmNftIndexerApiServiceUriProvider(
    private val environment: String
) : NftIndexerApiServiceUriProvider {

    override fun getUri(blockchain: String): URI {
        return URI.create(String.format("http://%s-%s-nft-api:8080", environment, blockchain))
    }

}
