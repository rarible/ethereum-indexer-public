package com.rarible.protocol.nft.core.configuration

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.protocol.nft.core.producer.ProducerFactory
import com.rarible.protocol.nft.core.producer.ProtocolNftEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ProducerConfiguration(
    private val properties: NftIndexerProperties,
    private val applicationEnvironmentInfo: ApplicationEnvironmentInfo
) {

    @Bean
    fun nftEventProducerFactory(): ProducerFactory {
        return ProducerFactory(
            kafkaReplicaSet = properties.kafkaReplicaSet,
            blockchain = properties.blockchain,
            environment = applicationEnvironmentInfo.name
        )
    }

    @Bean
    fun protocolNftEventPublisher(producerFactory: ProducerFactory): ProtocolNftEventPublisher {
        return ProtocolNftEventPublisher(
            producerFactory.createItemEventsProducer(),
            producerFactory.createInternalItemEventsProducer(),
            producerFactory.createOwnershipEventsProducer(),
            producerFactory.createItemActivityProducer()
        )
    }
}
