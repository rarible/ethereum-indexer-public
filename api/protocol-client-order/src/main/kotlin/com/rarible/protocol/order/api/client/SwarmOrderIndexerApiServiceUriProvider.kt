package com.rarible.protocol.order.api.client

import java.net.URI

class SwarmOrderIndexerApiServiceUriProvider(
    private val environment: String
) : OrderIndexerApiServiceUriProvider {

    override fun getUri(blockchain: String): URI {
        return URI.create(String.format("http://%s-%s-order-api:8080", environment, blockchain))
    }

}
