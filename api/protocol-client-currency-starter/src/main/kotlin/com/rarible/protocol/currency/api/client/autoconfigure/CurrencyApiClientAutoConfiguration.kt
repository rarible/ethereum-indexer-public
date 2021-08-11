package com.rarible.protocol.currency.api.client.autoconfigure

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.protocol.client.CompositeWebClientCustomizer
import com.rarible.protocol.client.DefaultProtocolWebClientCustomizer
import com.rarible.protocol.client.NoopWebClientCustomizer
import com.rarible.protocol.currency.api.client.CurrencyApiClientFactory
import com.rarible.protocol.currency.api.client.CurrencyApiServiceUriProvider
import com.rarible.protocol.currency.api.client.SwarmCurrencyApiServiceUriProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.context.annotation.Bean

const val CURRENCY_WEB_CLIENT_CUSTOMIZER = "CURRENCY_WEB_CLIENT_CUSTOMIZER"

class CurrencyApiClientAutoConfiguration(
    private val applicationEnvironmentInfo: ApplicationEnvironmentInfo
) {

    @Autowired(required = false)
    @Qualifier(CURRENCY_WEB_CLIENT_CUSTOMIZER)
    private var webClientCustomizer: WebClientCustomizer = NoopWebClientCustomizer()

    @Bean
    @ConditionalOnMissingBean(CurrencyApiServiceUriProvider::class)
    fun currencyApiServiceUriProvider(): CurrencyApiServiceUriProvider {
        return SwarmCurrencyApiServiceUriProvider(applicationEnvironmentInfo.name)
    }

    @Bean
    @ConditionalOnMissingBean(CurrencyApiClientFactory::class)
    fun currencyApiClientFactory(currencyApiServiceUriProvider: CurrencyApiServiceUriProvider): CurrencyApiClientFactory {
        val compositeWebClientCustomizer = CompositeWebClientCustomizer(listOf(DefaultProtocolWebClientCustomizer(), webClientCustomizer))
        return CurrencyApiClientFactory(currencyApiServiceUriProvider, compositeWebClientCustomizer)
    }
}
