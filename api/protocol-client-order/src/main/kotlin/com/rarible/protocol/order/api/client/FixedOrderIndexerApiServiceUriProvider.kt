package com.rarible.protocol.order.api.client

import java.net.URI

class FixedOrderIndexerApiServiceUriProvider(private val fixedURI: URI) : OrderIndexerApiServiceUriProvider {

    override fun getUri(blockchain: String): URI {
        return fixedURI
    }

}
