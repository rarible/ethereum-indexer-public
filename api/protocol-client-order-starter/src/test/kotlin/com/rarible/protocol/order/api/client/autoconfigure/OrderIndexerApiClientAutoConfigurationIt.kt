package com.rarible.protocol.order.api.client.autoconfigure

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.protocol.order.api.client.OrderIndexerApiClientFactory
import com.rarible.protocol.order.api.client.OrderIndexerApiServiceUriProvider
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
@Import(OrderIndexerApiClientAutoConfigurationIt.Configuration::class)
class OrderIndexerApiClientAutoConfigurationIt {

    @Autowired
    private lateinit var orderIndexerApiServiceUriProvider: OrderIndexerApiServiceUriProvider

    @Autowired
    private lateinit var orderIndexerApiClientFactory: OrderIndexerApiClientFactory

    @Autowired
    @Qualifier(ORDER_INDEXER_WEB_CLIENT_CUSTOMIZER)
    private lateinit var webClientCustomizer: WebClientCustomizer

    @Test
    fun `test default clients initialized`() {
        assertThat(orderIndexerApiServiceUriProvider).isNotNull
        assertThat(orderIndexerApiClientFactory).isNotNull

        every { webClientCustomizer.customize(any()) } returns Unit

        orderIndexerApiClientFactory.createOrderActivityApiClient("ethereum")

        verify { webClientCustomizer.customize(any()) }
    }

    @Test
    fun `test default client uri`() {
        val uri = orderIndexerApiServiceUriProvider.getUri("ethereum")
        assertThat(uri.toString()).isEqualTo("http://test-ethereum-order-api:8080")
    }

    @TestConfiguration
    internal class Configuration {

        @Bean
        @Qualifier(ORDER_INDEXER_WEB_CLIENT_CUSTOMIZER)
        fun webClientCustomizer(): WebClientCustomizer {
            return mockk()
        }

        @Bean
        fun applicationEnvironmentInfo(): ApplicationEnvironmentInfo {
            return ApplicationEnvironmentInfo("test", "test.com")
        }
    }
}
