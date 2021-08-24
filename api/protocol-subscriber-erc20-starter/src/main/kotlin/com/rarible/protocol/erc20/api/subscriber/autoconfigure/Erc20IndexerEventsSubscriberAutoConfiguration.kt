package com.rarible.protocol.erc20.api.subscriber.autoconfigure

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.protocol.erc20.api.subscriber.Erc20IndexerEventsConsumerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

@EnableConfigurationProperties(Erc20IndexerEventsSubscriberProperties::class)
class Erc20IndexerEventsSubscriberAutoConfiguration(
    private val applicationEnvironmentInfo: ApplicationEnvironmentInfo,
    private val properties: Erc20IndexerEventsSubscriberProperties
) {
    @Bean
    @ConditionalOnMissingBean(Erc20IndexerEventsConsumerFactory::class)
    fun erc20IndexerEventsConsumerFactory(): Erc20IndexerEventsConsumerFactory {
        return Erc20IndexerEventsConsumerFactory(
            brokerReplicaSet = properties.brokerReplicaSet,
            host = applicationEnvironmentInfo.host,
            environment = applicationEnvironmentInfo.name
        )
    }
}
