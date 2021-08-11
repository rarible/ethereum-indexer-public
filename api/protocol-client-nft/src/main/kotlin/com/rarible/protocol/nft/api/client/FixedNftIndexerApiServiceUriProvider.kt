package com.rarible.protocol.nft.api.client

import java.net.URI

class FixedNftIndexerApiServiceUriProvider(private val fixedURI: URI) : NftIndexerApiServiceUriProvider {

    override fun getUri(blockchain: String): URI {
        return fixedURI
    }

}
