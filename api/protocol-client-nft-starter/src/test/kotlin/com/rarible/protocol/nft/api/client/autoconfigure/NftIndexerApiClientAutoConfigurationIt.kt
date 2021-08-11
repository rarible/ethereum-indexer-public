package com.rarible.protocol.nft.api.client.autoconfigure

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.protocol.nft.api.client.NftIndexerApiClientFactory
import com.rarible.protocol.nft.api.client.NftIndexerApiServiceUriProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import

@SpringBootTest
@SpringBootConfiguration
@EnableAutoConfiguration
@Import(NftIndexerApiClientAutoConfigurationIt.Configuration::class)
class NftIndexerApiClientAutoConfigurationIt {

    @Autowired
    private lateinit var nftIndexerApiServiceUriProvider: NftIndexerApiServiceUriProvider

    @Autowired
    private lateinit var nftIndexerApiClientFactory: NftIndexerApiClientFactory

    @Autowired
    @Qualifier(NFT_INDEXER_WEB_CLIENT_CUSTOMIZER)
    private lateinit var webClientCustomizer: WebClientCustomizer

    @Test
    fun `test default clients initialized`() {
        assertThat(nftIndexerApiServiceUriProvider).isNotNull
        assertThat(nftIndexerApiClientFactory).isNotNull

        every { webClientCustomizer.customize(any()) } returns Unit

        nftIndexerApiClientFactory.createNftItemApiClient("ethereum")

        verify { webClientCustomizer.customize(any()) }
    }

    @Test
    fun `test default client uri`() {
        val uri = nftIndexerApiServiceUriProvider.getUri("ethereum")
        assertThat(uri.toString()).isEqualTo("http://test-ethereum-nft-api:8080")
    }

    @TestConfiguration
    internal class Configuration {

        @Bean
        @Qualifier(NFT_INDEXER_WEB_CLIENT_CUSTOMIZER)
        fun webClientCustomizer(): WebClientCustomizer {
            return mockk()
        }

        @Bean
        fun applicationEnvironmentInfo(): ApplicationEnvironmentInfo {
            return ApplicationEnvironmentInfo("test", "test.com")
        }
    }
}
