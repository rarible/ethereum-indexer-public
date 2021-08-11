package com.rarible.protocol.nftorder.api.client

import com.rarible.protocol.client.AbstractApiClientFactory
import com.rarible.protocol.client.ApiServiceUriProvider
import com.rarible.protocol.nftorder.api.ApiClient
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer

open class NftOrderApiClientFactory(
    uriProvider: ApiServiceUriProvider,
    webClientCustomizer: WebClientCustomizer
) : AbstractApiClientFactory(uriProvider, webClientCustomizer) {

    fun createNftOrderOwnershipApiClient(blockchain: String): NftOrderOwnershipControllerApi {
        return NftOrderOwnershipControllerApi(createApiClient(blockchain))
    }

    fun createNftOrderItemApiClient(blockchain: String): NftOrderItemControllerApi {
        return NftOrderItemControllerApi(createApiClient(blockchain))
    }

    fun createNftOrderActivityApiClient(blockchain: String): NftOrderActivityControllerApi {
        return NftOrderActivityControllerApi(createApiClient(blockchain))
    }

    fun createNftOrderLazyMintControllerApi(blockchain: String): NftOrderLazyMintControllerApi {
        return NftOrderLazyMintControllerApi(createApiClient(blockchain))
    }

    fun createNftOrderCollectionControllerApi(blockchain: String): NftOrderCollectionControllerApi {
        return NftOrderCollectionControllerApi(createApiClient(blockchain))
    }

    private fun createApiClient(blockchain: String): ApiClient {
        return ApiClient(webClientCustomizer)
            .setBasePath(getBasePath(blockchain))
    }

}

