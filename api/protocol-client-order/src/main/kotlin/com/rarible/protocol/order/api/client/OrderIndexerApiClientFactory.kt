package com.rarible.protocol.order.api.client

import com.rarible.protocol.client.AbstractApiClientFactory
import com.rarible.protocol.client.ApiServiceUriProvider
import com.rarible.protocol.order.api.ApiClient
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer

open class OrderIndexerApiClientFactory(
    uriProvider: ApiServiceUriProvider,
    webClientCustomizer: WebClientCustomizer
) : AbstractApiClientFactory(uriProvider, webClientCustomizer) {

    fun createOrderSignatureApiClient(blockchain: String): OrderSignatureControllerApi {
        return OrderSignatureControllerApi(createApiClient(blockchain))
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

    fun createAuctionApiClient(blockchain: String): AuctionControllerApi {
        return AuctionControllerApi(createApiClient(blockchain))
    }

    private fun createApiClient(blockchain: String): ApiClient {
        return ApiClient(webClientCustomizer)
            .setBasePath(getBasePath(blockchain))
    }
}

