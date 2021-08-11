package com.rarible.protocol.currency.api.client

import com.rarible.protocol.client.AbstractApiClientFactory
import com.rarible.protocol.client.ApiServiceUriProvider
import com.rarible.protocol.currency.api.ApiClient
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer

open class CurrencyApiClientFactory(
    uriProvider: ApiServiceUriProvider,
    webClientCustomizer: WebClientCustomizer
) : AbstractApiClientFactory(uriProvider, webClientCustomizer) {

    fun createCurrencyApiClient(blockchain: String): CurrencyControllerApi {
        return CurrencyControllerApi(createApiClient(blockchain))
    }

    private fun createApiClient(blockchain: String): ApiClient {
        return ApiClient(webClientCustomizer)
            .setBasePath(getBasePath(blockchain))
    }

}

