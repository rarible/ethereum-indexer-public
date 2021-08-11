package com.rarible.protocol.nftorder.api.client.autoconfigure

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.protocol.nftorder.api.client.NftOrderApiClientFactory
import com.rarible.protocol.nftorder.api.client.NftOrderApiServiceUriProvider
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
@Import(NftOrderApiClientAutoConfigurationIt.Configuration::class)
class NftOrderApiClientAutoConfigurationIt {

    @Autowired
    private lateinit var nftOrderApiServiceUriProvider: NftOrderApiServiceUriProvider

    @Autowired
    private lateinit var nftOrderApiClientFactory: NftOrderApiClientFactory

    @Autowired
    @Qualifier(NFT_ORDER_WEB_CLIENT_CUSTOMIZER)
    private lateinit var webClientCustomizer: WebClientCustomizer

    @Test
    fun `test default clients initialized`() {
        assertThat(nftOrderApiServiceUriProvider).isNotNull
        assertThat(nftOrderApiClientFactory).isNotNull

        every { webClientCustomizer.customize(any()) } returns Unit

        nftOrderApiClientFactory.createNftOrderActivityApiClient("ethereum")

        verify { webClientCustomizer.customize(any()) }
    }

    @Test
    fun `test default client uri`() {
        val uri = nftOrderApiServiceUriProvider.getUri("ethereum")
        assertThat(uri.toString()).isEqualTo("http://test-ethereum-nft-order-api:8080")
    }

    @TestConfiguration
    internal class Configuration {

        @Bean
        @Qualifier(NFT_ORDER_WEB_CLIENT_CUSTOMIZER)
        fun webClientCustomizer(): WebClientCustomizer {
            return mockk()
        }

        @Bean
        fun applicationEnvironmentInfo(): ApplicationEnvironmentInfo {
            return ApplicationEnvironmentInfo("test", "test.com")
        }
    }
}
