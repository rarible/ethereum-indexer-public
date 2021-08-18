package com.rarible.protocol.gateway.api.client.autoconfigure

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.protocol.gateway.api.client.GatewayApiClientFactory
import com.rarible.protocol.gateway.client.GatewayApiServiceUriProvider
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
@Import(GatewayApiClientAutoConfigurationIt.Configuration::class)
class GatewayApiClientAutoConfigurationIt {

    @Autowired
    private lateinit var gatewayApiServiceUriProvider: GatewayApiServiceUriProvider

    @Autowired
    private lateinit var gatewayApiClientFactory: GatewayApiClientFactory

    @Autowired
    @Qualifier(GATEWAY_WEB_CLIENT_CUSTOMIZER)
    private lateinit var webClientCustomizer: WebClientCustomizer

    @Test
    fun `test default clients initialized`() {
        assertThat(gatewayApiServiceUriProvider).isNotNull
        assertThat(gatewayApiClientFactory).isNotNull

        every { webClientCustomizer.customize(any()) } returns Unit

        gatewayApiClientFactory.createNftOrderActivityApiClient("ethereum")

        verify { webClientCustomizer.customize(any()) }
    }

    @Test
    fun `test default client uri`() {
        val uri = gatewayApiServiceUriProvider.getUri("ethereum")
        assertThat(uri.toString()).isEqualTo("http://test-ethereum-gateway:8080")
    }

    @TestConfiguration
    internal class Configuration {

        @Bean
        @Qualifier(GATEWAY_WEB_CLIENT_CUSTOMIZER)
        fun webClientCustomizer(): WebClientCustomizer {
            return mockk()
        }

        @Bean
        fun applicationEnvironmentInfo(): ApplicationEnvironmentInfo {
            return ApplicationEnvironmentInfo("test", "test.com")
        }
    }
}
