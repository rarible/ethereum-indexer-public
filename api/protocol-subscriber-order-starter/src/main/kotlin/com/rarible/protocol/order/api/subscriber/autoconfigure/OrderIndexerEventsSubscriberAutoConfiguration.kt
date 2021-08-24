package com.rarible.protocol.order.api.subscriber.autoconfigure

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.protocol.order.api.subscriber.OrderIndexerEventsConsumerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

@EnableConfigurationProperties(OrderIndexerEventsSubscriberProperties::class)
class OrderIndexerEventsSubscriberAutoConfiguration(
    private val applicationEnvironmentInfo: ApplicationEnvironmentInfo,
    private val properties: OrderIndexerEventsSubscriberProperties
) {
    @Bean
    @ConditionalOnMissingBean(OrderIndexerEventsConsumerFactory::class)
    fun orderIndexerEventsConsumerFactory(): OrderIndexerEventsConsumerFactory {
        return OrderIndexerEventsConsumerFactory(
            brokerReplicaSet = properties.brokerReplicaSet,
            host = applicationEnvironmentInfo.host,
            environment = applicationEnvironmentInfo.name
        )
    }
}
