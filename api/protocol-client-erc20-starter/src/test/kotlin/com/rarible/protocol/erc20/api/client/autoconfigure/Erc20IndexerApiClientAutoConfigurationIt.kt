package com.rarible.protocol.erc20.api.client.autoconfigure

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.protocol.erc20.api.client.Erc20IndexerApiClientFactory
import com.rarible.protocol.erc20.api.client.Erc20IndexerApiServiceUriProvider
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
@Import(Erc20IndexerApiClientAutoConfigurationIt.Configuration::class)
class Erc20IndexerApiClientAutoConfigurationIt {

    @Autowired
    private lateinit var erc20IndexerApiServiceUriProvider: Erc20IndexerApiServiceUriProvider

    @Autowired
    private lateinit var erc20IndexerApiClientFactory: Erc20IndexerApiClientFactory

    @Autowired
    @Qualifier(ERC20_INDEXER_WEB_CLIENT_CUSTOMIZER)
    private lateinit var webClientCustomizer: WebClientCustomizer

    @Test
    fun `test default clients initialized`() {
        assertThat(erc20IndexerApiServiceUriProvider).isNotNull
        assertThat(erc20IndexerApiClientFactory).isNotNull

        every { webClientCustomizer.customize(any()) } returns Unit

        erc20IndexerApiClientFactory.createErc20BalanceApiClient("ethereum")

        verify { webClientCustomizer.customize(any()) }
    }

    @Test
    fun `test default client uri`() {
        val uri = erc20IndexerApiServiceUriProvider.getUri("ethereum")
        assertThat(uri.toString()).isEqualTo("http://test-ethereum-erc20-api:8080")
    }

    @TestConfiguration
    internal class Configuration {

        @Bean
        @Qualifier(ERC20_INDEXER_WEB_CLIENT_CUSTOMIZER)
        fun webClientCustomizer(): WebClientCustomizer {
            return mockk()
        }

        @Bean
        fun applicationEnvironmentInfo(): ApplicationEnvironmentInfo {
            return ApplicationEnvironmentInfo("test", "test.com")
        }
    }
}