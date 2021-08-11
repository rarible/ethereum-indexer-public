package com.rarible.protocol.currency.api.client.autoconfigure

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.protocol.currency.api.client.CurrencyApiClientFactory
import com.rarible.protocol.currency.api.client.CurrencyApiServiceUriProvider
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
@Import(CurrencyApiClientAutoConfigurationIt.Configuration::class)
class CurrencyApiClientAutoConfigurationIt {

    @Autowired
    private lateinit var currencyApiServiceUriProvider: CurrencyApiServiceUriProvider

    @Autowired
    private lateinit var currencyApiClientFactory: CurrencyApiClientFactory

    @Autowired
    @Qualifier(CURRENCY_WEB_CLIENT_CUSTOMIZER)
    private lateinit var webClientCustomizer: WebClientCustomizer

    @Test
    fun `test default clients initialized`() {
        assertThat(currencyApiServiceUriProvider).isNotNull
        assertThat(currencyApiClientFactory).isNotNull

        every { webClientCustomizer.customize(any()) } returns Unit

        currencyApiClientFactory.createCurrencyApiClient("ethereum")

        verify { webClientCustomizer.customize(any()) }
    }

    @Test
    fun `test default client uri`() {
        val uri = currencyApiServiceUriProvider.getUri("ANYTHING")
        assertThat(uri.toString()).isEqualTo("http://test-currency-api:8080")
    }

    @TestConfiguration
    internal class Configuration {

        @Bean
        @Qualifier(CURRENCY_WEB_CLIENT_CUSTOMIZER)
        fun webClientCustomizer(): WebClientCustomizer {
            return mockk()
        }

        @Bean
        fun applicationEnvironmentInfo(): ApplicationEnvironmentInfo {
            return ApplicationEnvironmentInfo("test", "test.com")
        }
    }
}