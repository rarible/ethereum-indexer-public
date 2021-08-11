package com.rarible.protocol.unlockable.api.client.autoconfigure

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.protocol.unlockable.api.client.UnlockableApiClientFactory
import com.rarible.protocol.unlockable.api.client.UnlockableApiServiceUriProvider
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
@Import(UnlockableApiClientAutoConfigurationIt.Configuration::class)
class UnlockableApiClientAutoConfigurationIt {

    @Autowired
    private lateinit var unlockableApiServiceUriProvider: UnlockableApiServiceUriProvider

    @Autowired
    private lateinit var unlockableApiClientFactory: UnlockableApiClientFactory

    @Autowired
    @Qualifier(UNLOCKABLE_WEB_CLIENT_CUSTOMIZER)
    private lateinit var webClientCustomizer: WebClientCustomizer

    @Test
    fun `test default clients initialized`() {
        assertThat(unlockableApiServiceUriProvider).isNotNull
        assertThat(unlockableApiClientFactory).isNotNull

        every { webClientCustomizer.customize(any()) } returns Unit

        unlockableApiClientFactory.createUnlockableApiClient("ethereum")

        verify { webClientCustomizer.customize(any()) }
    }

    @Test
    fun `test default client uri`() {
        val uri = unlockableApiServiceUriProvider.getUri("ethereum")
        assertThat(uri.toString()).isEqualTo("http://test-ethereum-unlockable-api:8080")
    }

    @TestConfiguration
    internal class Configuration {

        @Bean
        @Qualifier(UNLOCKABLE_WEB_CLIENT_CUSTOMIZER)
        fun webClientCustomizer(): WebClientCustomizer {
            return mockk()
        }

        @Bean
        fun applicationEnvironmentInfo(): ApplicationEnvironmentInfo {
            return ApplicationEnvironmentInfo("test", "test.com")
        }
    }
}