package com.rarible.protocol.order.core.configuration

import com.rarible.protocol.currency.api.client.CurrencyApiClientFactory
import com.rarible.protocol.currency.api.client.CurrencyControllerApi
import com.rarible.protocol.erc20.api.client.BalanceControllerApi
import com.rarible.protocol.erc20.api.client.Erc20IndexerApiClientFactory
import com.rarible.protocol.nft.api.client.NftCollectionControllerApi
import com.rarible.protocol.nft.api.client.NftIndexerApiClientFactory
import com.rarible.protocol.nft.api.client.NftItemControllerApi
import com.rarible.protocol.nft.api.client.NftOwnershipControllerApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ApiClientConfiguration(
    private val indexerProperties: OrderIndexerProperties,
    private val nftIndexerApiClientFactory: NftIndexerApiClientFactory,
    private val erc20IndexerApiClientFactory: Erc20IndexerApiClientFactory,
    private val currencyApiClientFactory: CurrencyApiClientFactory
) {

    @Bean
    fun nftOwnershipApi(): NftOwnershipControllerApi {
        return nftIndexerApiClientFactory.createNftOwnershipApiClient(indexerProperties.blockchain.name)
    }

    @Bean
    fun nftItemApi(): NftItemControllerApi {
        return nftIndexerApiClientFactory.createNftItemApiClient(indexerProperties.blockchain.name)
    }

    @Bean
    fun nftCollectionApi(): NftCollectionControllerApi {
        return nftIndexerApiClientFactory.createNftCollectionApiClient(indexerProperties.blockchain.name)
    }

    @Bean
    fun erc20BalanceApi(): BalanceControllerApi {
        return erc20IndexerApiClientFactory.createBalanceApiClient(indexerProperties.blockchain.name)
    }

    @Bean
    fun currencyApi(): CurrencyControllerApi {
        return currencyApiClientFactory.createCurrencyApiClient()
    }
}
