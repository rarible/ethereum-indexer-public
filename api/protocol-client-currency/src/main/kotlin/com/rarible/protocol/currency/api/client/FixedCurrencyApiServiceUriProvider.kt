package com.rarible.protocol.currency.api.client

import java.net.URI

class FixedCurrencyApiServiceUriProvider(private val fixedURI: URI) : CurrencyApiServiceUriProvider {

    override fun getUri(blockchain: String): URI {
        return fixedURI
    }

}
