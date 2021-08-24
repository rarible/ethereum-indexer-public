package com.rarible.protocol.unlockable.api.subscriber.autoconfigure

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.protocol.unlockable.api.subscriber.UnlockableEventsConsumerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

@EnableConfigurationProperties(UnlockableEventsSubscriberProperties::class)
class UnlockableEventsSubscriberAutoConfiguration(
    private val applicationEnvironmentInfo: ApplicationEnvironmentInfo,
    private val properties: UnlockableEventsSubscriberProperties
) {
    @Bean
    @ConditionalOnMissingBean(UnlockableEventsConsumerFactory::class)
    fun unlockableEventsConsumerFactory(): UnlockableEventsConsumerFactory {
        return UnlockableEventsConsumerFactory(
            brokerReplicaSet = properties.brokerReplicaSet,
            host = applicationEnvironmentInfo.host,
            environment = applicationEnvironmentInfo.name
        )
    }
}
