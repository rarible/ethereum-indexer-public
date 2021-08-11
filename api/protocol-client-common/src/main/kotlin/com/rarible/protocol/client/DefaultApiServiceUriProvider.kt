package com.rarible.protocol.client

import java.net.URI

open class DefaultApiServiceUriProvider(
    protected val environment: String,
    protected val service: String
) : ApiServiceUriProvider {
    override fun getUri(blockchain: String): URI {
        return URI.create(String.format("http://%s-%s-%s:8080", environment, blockchain, service))
    }
}
