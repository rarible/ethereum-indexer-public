package com.rarible.protocol.erc20.api.client

import java.net.URI

class SwarmErc20IndexerApiServiceUriProvider(
    private val environment: String
) : Erc20IndexerApiServiceUriProvider {

    override fun getUri(blockchain: String): URI {
        return URI.create(String.format("http://%s-%s-erc20-api:8080", environment, blockchain))
    }
}
