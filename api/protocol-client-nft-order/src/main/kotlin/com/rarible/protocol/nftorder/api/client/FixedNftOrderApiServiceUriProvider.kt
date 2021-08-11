package com.rarible.protocol.nftorder.api.client

import java.net.URI

class FixedNftOrderApiServiceUriProvider(private val fixedURI: URI) : NftOrderApiServiceUriProvider {

    override fun getUri(blockchain: String): URI {
        return fixedURI
    }
}