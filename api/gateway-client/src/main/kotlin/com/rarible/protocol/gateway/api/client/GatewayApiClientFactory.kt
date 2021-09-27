package com.rarible.protocol.gateway.api.client

import com.rarible.protocol.client.AbstractApiClientFactory
import com.rarible.protocol.client.ApiServiceUriProvider
import com.rarible.protocol.gateway.api.ApiClient
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer

class GatewayApiClientFactory(
    uriProvider: ApiServiceUriProvider,
    webClientCustomizer: WebClientCustomizer
) : AbstractApiClientFactory(uriProvider, webClientCustomizer) {

    fun createErc20BalanceApiClient(blockchain: String): Erc20BalanceControllerApi {
        return Erc20BalanceControllerApi(createApiClient(blockchain))
    }

    fun createErc20TokenApiClient(blockchain: String): Erc20TokenControllerApi {
        return Erc20TokenControllerApi(createApiClient(blockchain))
    }

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

    fun createNftOrderOwnershipApiClient(blockchain: String): NftOrderOwnershipControllerApi {
        return NftOrderOwnershipControllerApi(createApiClient(blockchain))
    }

    fun createNftOrderItemApiClient(blockchain: String): NftOrderItemControllerApi {
        return NftOrderItemControllerApi(createApiClient(blockchain))
    }

    fun createNftOrderActivityApiClient(blockchain: String): NftOrderActivityControllerApi {
        return NftOrderActivityControllerApi(createApiClient(blockchain))
    }

    fun createNftOrderCollectionApiClient(blockchain: String): NftOrderCollectionControllerApi {
        return NftOrderCollectionControllerApi(createApiClient(blockchain))
    }

    fun createNftOrderLazyMintApiClient(blockchain: String): NftOrderLazyMintControllerApi {
        return NftOrderLazyMintControllerApi(createApiClient(blockchain))
    }

    fun createOrderApiClient(blockchain: String): OrderControllerApi {
        return OrderControllerApi(createApiClient(blockchain))
    }

    fun createOrderBidApiClient(blockchain: String): OrderBidControllerApi {
        return OrderBidControllerApi(createApiClient(blockchain))
    }

    fun createOrderEncodeApiClient(blockchain: String): OrderEncodeControllerApi {
        return OrderEncodeControllerApi(createApiClient(blockchain))
    }

    fun createOrderActivityApiClient(blockchain: String): OrderActivityControllerApi {
        return OrderActivityControllerApi(createApiClient(blockchain))
    }

    fun createOrderTransactionApiClient(blockchain: String): OrderTransactionControllerApi {
        return OrderTransactionControllerApi(createApiClient(blockchain))
    }

    fun createOrderAggregationApiClient(blockchain: String): OrderAggregationControllerApi {
        return OrderAggregationControllerApi(createApiClient(blockchain))
    }

    fun createUnlockableApiClient(blockchain: String): LockControllerApi {
        return LockControllerApi(createApiClient(blockchain))
    }

    fun createGatewayControllerApiClient(blockchain: String): GatewayControllerApi {
        return GatewayControllerApi(createApiClient(blockchain))
    }

    private fun createApiClient(blockchain: String): ApiClient {
        return ApiClient(webClientCustomizer)
            .setBasePath(getBasePath(blockchain))
    }

}

