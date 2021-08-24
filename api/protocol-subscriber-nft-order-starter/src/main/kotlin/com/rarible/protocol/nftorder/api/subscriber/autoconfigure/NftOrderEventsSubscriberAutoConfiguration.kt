package com.rarible.protocol.nftorder.api.subscriber.autoconfigure

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.protocol.nftorder.api.subscriber.NftOrderEventsConsumerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

@EnableConfigurationProperties(NftOrderEventsSubscriberProperties::class)
class NftOrderEventsSubscriberAutoConfiguration(
    private val applicationEnvironmentInfo: ApplicationEnvironmentInfo,
    private val properties: NftOrderEventsSubscriberProperties
) {
    @Bean
    @ConditionalOnMissingBean(NftOrderEventsConsumerFactory::class)
    fun nftOrderEventsConsumerFactory(): NftOrderEventsConsumerFactory {
        return NftOrderEventsConsumerFactory(
            brokerReplicaSet = properties.brokerReplicaSet,
            host = applicationEnvironmentInfo.host,
            environment = applicationEnvironmentInfo.name
        )
    }
}
