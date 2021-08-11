package com.rarible.protocol.unlockable.api.client

import com.rarible.protocol.client.AbstractApiClientFactory
import com.rarible.protocol.client.ApiServiceUriProvider
import com.rarible.protocol.unlockable.api.ApiClient
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer

open class UnlockableApiClientFactory(
    uriProvider: ApiServiceUriProvider,
    webClientCustomizer: WebClientCustomizer
) : AbstractApiClientFactory(uriProvider, webClientCustomizer) {

    fun createUnlockableApiClient(blockchain: String): LockControllerApi {
        return LockControllerApi(createApiClient(blockchain))
    }

    private fun createApiClient(blockchain: String): ApiClient {
        return ApiClient(webClientCustomizer)
            .setBasePath(getBasePath(blockchain))
    }

}

