package com.rarible.protocol.nft.api.client.autoconfigure

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.protocol.client.CompositeWebClientCustomizer
import com.rarible.protocol.client.DefaultProtocolWebClientCustomizer
import com.rarible.protocol.client.NoopWebClientCustomizer
import com.rarible.protocol.nft.api.client.NftIndexerApiClientFactory
import com.rarible.protocol.nft.api.client.NftIndexerApiServiceUriProvider
import com.rarible.protocol.nft.api.client.SwarmNftIndexerApiServiceUriProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.context.annotation.Bean

const val NFT_INDEXER_WEB_CLIENT_CUSTOMIZER = "NFT_INDEXER_WEB_CLIENT_CUSTOMIZER"

class NftIndexerApiClientAutoConfiguration(
    private val applicationEnvironmentInfo: ApplicationEnvironmentInfo
) {

    @Autowired(required = false)
    @Qualifier(NFT_INDEXER_WEB_CLIENT_CUSTOMIZER)
    private var webClientCustomizer: WebClientCustomizer = NoopWebClientCustomizer()

    @Bean
    @ConditionalOnMissingBean(NftIndexerApiServiceUriProvider::class)
    fun nftIndexerApiServiceUriProvider(): NftIndexerApiServiceUriProvider {
        return SwarmNftIndexerApiServiceUriProvider(applicationEnvironmentInfo.name)
    }

    @Bean
    @ConditionalOnMissingBean(NftIndexerApiClientFactory::class)
    fun nftIndexerApiClientFactory(nftIndexerApiServiceUriProvider: NftIndexerApiServiceUriProvider): NftIndexerApiClientFactory {
        val compositeWebClientCustomizer = CompositeWebClientCustomizer(listOf(DefaultProtocolWebClientCustomizer(), webClientCustomizer))
        return NftIndexerApiClientFactory(nftIndexerApiServiceUriProvider, compositeWebClientCustomizer)
    }
}