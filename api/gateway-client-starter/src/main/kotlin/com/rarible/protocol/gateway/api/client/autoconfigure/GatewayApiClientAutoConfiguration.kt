package com.rarible.protocol.gateway.api.client.autoconfigure

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.protocol.client.CompositeWebClientCustomizer
import com.rarible.protocol.client.DefaultProtocolWebClientCustomizer
import com.rarible.protocol.client.NoopWebClientCustomizer
import com.rarible.protocol.gateway.api.client.GatewayApiClientFactory
import com.rarible.protocol.gateway.client.GatewayApiServiceUriProvider
import com.rarible.protocol.gateway.client.SwarmGatewayApiServiceUriProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.context.annotation.Bean

const val GATEWAY_WEB_CLIENT_CUSTOMIZER = "GATEWAY_WEB_CLIENT_CUSTOMIZER"

class GatewayApiClientAutoConfiguration(
    private val applicationEnvironmentInfo: ApplicationEnvironmentInfo
) {

    @Autowired(required = false)
    @Qualifier(GATEWAY_WEB_CLIENT_CUSTOMIZER)
    private var webClientCustomizer: WebClientCustomizer = NoopWebClientCustomizer()

    @Bean
    @ConditionalOnMissingBean(GatewayApiServiceUriProvider::class)
    fun gatewayApiServiceUriProvider(): GatewayApiServiceUriProvider {
        return SwarmGatewayApiServiceUriProvider(applicationEnvironmentInfo.name)
    }

    @Bean
    @ConditionalOnMissingBean(GatewayApiClientFactory::class)
    fun gatewayApiClientFactory(gatewayApiApiServiceUriProvider: GatewayApiServiceUriProvider): GatewayApiClientFactory {
        val compositeWebClientCustomizer =
            CompositeWebClientCustomizer(listOf(DefaultProtocolWebClientCustomizer(), webClientCustomizer))
        return GatewayApiClientFactory(gatewayApiApiServiceUriProvider, compositeWebClientCustomizer)
    }
}