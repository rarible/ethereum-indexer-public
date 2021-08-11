package com.rarible.protocol.client

import java.net.URI

open class FixedApiServiceUriProvider(
    private val fixedURI: URI
) : ApiServiceUriProvider {

    override fun getUri(blockchain: String): URI {
        return fixedURI
    }
}
