package com.rarible.protocol.nft.api.subscriber.autoconfigure

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.protocol.nft.api.subscriber.NftIndexerEventsConsumerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

@EnableConfigurationProperties(NftIndexerEventsSubscriberProperties::class)
class NftIndexerEventsSubscriberAutoConfiguration(
    private val applicationEnvironmentInfo: ApplicationEnvironmentInfo,
    private val properties: NftIndexerEventsSubscriberProperties
) {
    @Bean
    @ConditionalOnMissingBean(NftIndexerEventsConsumerFactory::class)
    fun nftIndexerEventsConsumerFactory(): NftIndexerEventsConsumerFactory {
        return NftIndexerEventsConsumerFactory(
            brokerReplicaSet = properties.brokerReplicaSet,
            host = applicationEnvironmentInfo.host,
            environment = applicationEnvironmentInfo.name
        )
    }
}
