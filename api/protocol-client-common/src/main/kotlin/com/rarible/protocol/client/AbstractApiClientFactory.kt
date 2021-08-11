package com.rarible.protocol.client

import org.springframework.boot.web.reactive.function.client.WebClientCustomizer

open class AbstractApiClientFactory(
    private val uriProvider: ApiServiceUriProvider,
    protected val webClientCustomizer: WebClientCustomizer
) {

    protected fun getBasePath(blockchain: String): String {
        return uriProvider.getUri(blockchain).toASCIIString()
    }
}
