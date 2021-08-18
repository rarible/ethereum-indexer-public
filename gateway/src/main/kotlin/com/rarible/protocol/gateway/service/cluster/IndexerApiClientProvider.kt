package com.rarible.protocol.gateway.service.cluster

import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.nft.api.client.NftIndexerApiClientFactory
import com.rarible.protocol.nft.api.client.NftTransactionControllerApi
import com.rarible.protocol.order.api.client.OrderIndexerApiClientFactory
import com.rarible.protocol.order.api.client.OrderTransactionControllerApi
import org.springframework.stereotype.Component

@Component
class IndexerApiClientProvider(
    private val nftIndexerApiClientFactory: NftIndexerApiClientFactory,
    private val orderIndexerApiClientFactory: OrderIndexerApiClientFactory
) {

    private val orderTransactionApiClients = Blockchain.values().associate { blockchain ->
        blockchain to orderIndexerApiClientFactory.createOrderTransactionApiClient(blockchain.value)
    }

    private val nftTransactionApiClients = Blockchain.values().associate { blockchain ->
        blockchain to nftIndexerApiClientFactory.createNftTransactionApiClient(blockchain.value)
    }

    fun getNftTransactionApiClient(blockchain: Blockchain): NftTransactionControllerApi {
        return nftTransactionApiClients[blockchain] ?: error("Can't get NftTransactionApi for $blockchain")
    }

    fun getOrderTransactionApiClient(blockchain: Blockchain): OrderTransactionControllerApi {
        return orderTransactionApiClients[blockchain] ?: error("Can't get OrderTransactionApi for $blockchain")
    }
}