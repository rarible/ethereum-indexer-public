package com.rarible.protocol.currency.api.client

import java.net.URI

class SwarmCurrencyApiServiceUriProvider(
    private val environment: String
) : CurrencyApiServiceUriProvider {

    override fun getUri(blockchain: String): URI {
        return URI.create(String.format("http://%s-currency-api:8080", environment))
    }

}
