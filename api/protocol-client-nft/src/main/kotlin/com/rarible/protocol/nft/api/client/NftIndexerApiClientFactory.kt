package com.rarible.protocol.nft.api.client

import com.rarible.protocol.client.AbstractApiClientFactory
import com.rarible.protocol.client.ApiServiceUriProvider
import com.rarible.protocol.nft.api.ApiClient
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer

open class NftIndexerApiClientFactory(
    uriProvider: ApiServiceUriProvider,
    webClientCustomizer: WebClientCustomizer
) : AbstractApiClientFactory(uriProvider, webClientCustomizer) {

    fun createNftOwnershipApiClient(blockchain: String): NftOwnershipControllerApi {
        return NftOwnershipControllerApi(createApiClient(blockchain))
    }

    fun createNftItemApiClient(blockchain: String): NftItemControllerApi {
        return NftItemControllerApi(createApiClient(blockchain))
    }

    fun createNftActivityApiClient(blockchain: String): NftActivityControllerApi {
        return NftActivityControllerApi(createApiClient(blockchain))
    }

    fun createNftCollectionApiClient(blockchain: String): NftCollectionControllerApi {
        return NftCollectionControllerApi(createApiClient(blockchain))
    }

    fun createNftMintApiClient(blockchain: String): NftLazyMintControllerApi {
        return NftLazyMintControllerApi(createApiClient(blockchain))
    }

    fun createNftTransactionApiClient(blockchain: String): NftTransactionControllerApi {
        return NftTransactionControllerApi(createApiClient(blockchain))
    }

    private fun createApiClient(blockchain: String): ApiClient {
        return ApiClient(webClientCustomizer)
            .setBasePath(getBasePath(blockchain))
    }
}

