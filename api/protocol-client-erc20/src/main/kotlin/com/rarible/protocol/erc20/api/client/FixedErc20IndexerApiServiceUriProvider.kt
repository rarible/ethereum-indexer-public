package com.rarible.protocol.erc20.api.client

import java.net.URI

class FixedErc20IndexerApiServiceUriProvider(private val fixedURI: URI) : Erc20IndexerApiServiceUriProvider {

    override fun getUri(blockchain: String): URI {
        return fixedURI
    }

}
