package com.rarible.protocol.gateway.client

import java.net.URI

class FixedGatewayApiServiceUriProvider(private val fixedURI: URI) : GatewayApiServiceUriProvider {

    override fun getUri(blockchain: String): URI {
        return fixedURI
    }

}