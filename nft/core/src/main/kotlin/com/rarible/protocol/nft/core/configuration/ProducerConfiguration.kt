package com.rarible.protocol.nft.core.configuration

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.protocol.nft.core.producer.ProducerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@ComponentScan(basePackageClasses = [com.rarible.protocol.nft.core.producer.Package::class])
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
    fun collectionEventsProducer(producerFactory: ProducerFactory) = producerFactory.createCollectionEventsProducer()

    @Bean
    fun internalCollectionEventsProducer(producerFactory: ProducerFactory) = producerFactory.createInternalCollectionEventsProducer()

    @Bean
    fun itemEventsProducer(producerFactory: ProducerFactory) = producerFactory.createItemEventsProducer()

    @Bean
    fun internalItemEventsProducer(producerFactory: ProducerFactory) = producerFactory.createInternalItemEventsProducer()

    @Bean
    fun ownershipEventsProducer(producerFactory: ProducerFactory) = producerFactory.createOwnershipEventsProducer()

    @Bean
    fun itemActivityProducer(producerFactory: ProducerFactory) = producerFactory.createItemActivityProducer()
}
