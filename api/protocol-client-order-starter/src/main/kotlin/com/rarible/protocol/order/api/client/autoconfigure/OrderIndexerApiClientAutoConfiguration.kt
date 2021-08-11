package com.rarible.protocol.order.api.client.autoconfigure

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.protocol.client.CompositeWebClientCustomizer
import com.rarible.protocol.client.DefaultProtocolWebClientCustomizer
import com.rarible.protocol.client.NoopWebClientCustomizer
import com.rarible.protocol.order.api.client.OrderIndexerApiClientFactory
import com.rarible.protocol.order.api.client.OrderIndexerApiServiceUriProvider
import com.rarible.protocol.order.api.client.SwarmOrderIndexerApiServiceUriProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.context.annotation.Bean

const val ORDER_INDEXER_WEB_CLIENT_CUSTOMIZER = "ORDER_INDEXER_WEB_CLIENT_CUSTOMIZER"

class OrderIndexerApiClientAutoConfiguration(
    private val applicationEnvironmentInfo: ApplicationEnvironmentInfo
) {

    @Autowired(required = false)
    @Qualifier(ORDER_INDEXER_WEB_CLIENT_CUSTOMIZER)
    private var webClientCustomizer: WebClientCustomizer = NoopWebClientCustomizer()

    @Bean
    @ConditionalOnMissingBean(OrderIndexerApiServiceUriProvider::class)
    fun orderIndexerApiServiceUriProvider(): OrderIndexerApiServiceUriProvider {
        return SwarmOrderIndexerApiServiceUriProvider(applicationEnvironmentInfo.name)
    }

    @Bean
    @ConditionalOnMissingBean(OrderIndexerApiClientFactory::class)
    fun orderIndexerApiClientFactory(orderIndexerApiServiceUriProvider: OrderIndexerApiServiceUriProvider): OrderIndexerApiClientFactory {
        val compositeWebClientCustomizer = CompositeWebClientCustomizer(listOf(DefaultProtocolWebClientCustomizer(), webClientCustomizer))
        return OrderIndexerApiClientFactory(orderIndexerApiServiceUriProvider, compositeWebClientCustomizer)
    }
}
