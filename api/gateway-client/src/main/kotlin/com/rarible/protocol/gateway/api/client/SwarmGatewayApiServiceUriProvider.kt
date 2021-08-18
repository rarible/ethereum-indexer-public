package com.rarible.protocol.gateway.client

import java.net.URI

class SwarmGatewayApiServiceUriProvider(
    private val environment: String
) : GatewayApiServiceUriProvider {

    override fun getUri(blockchain: String): URI {
        return URI.create(String.format("http://%s-%s-gateway:8080", environment, blockchain))
    }
}
