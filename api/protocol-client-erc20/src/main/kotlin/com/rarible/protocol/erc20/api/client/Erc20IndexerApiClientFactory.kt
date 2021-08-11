package com.rarible.protocol.erc20.api.client

import com.rarible.protocol.client.AbstractApiClientFactory
import com.rarible.protocol.client.ApiServiceUriProvider
import com.rarible.protocol.erc20.api.ApiClient
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer

open class Erc20IndexerApiClientFactory(
    uriProvider: ApiServiceUriProvider,
    webClientCustomizer: WebClientCustomizer
) : AbstractApiClientFactory(uriProvider, webClientCustomizer) {

    fun createErc20BalanceApiClient(blockchain: String): Erc20BalanceControllerApi {
        return Erc20BalanceControllerApi(createApiClient(blockchain))
    }

    fun createErc20TokenApiClient(blockchain: String): Erc20TokenControllerApi {
        return Erc20TokenControllerApi(createApiClient(blockchain))
    }

    private fun createApiClient(blockchain: String): ApiClient {
        return ApiClient(webClientCustomizer)
            .setBasePath(getBasePath(blockchain))
    }
}

