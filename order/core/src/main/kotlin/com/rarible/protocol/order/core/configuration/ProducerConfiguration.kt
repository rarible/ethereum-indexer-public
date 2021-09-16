package com.rarible.protocol.order.core.configuration

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.protocol.order.core.producer.ProducerFactory
import com.rarible.protocol.order.core.producer.ProtocolOrderPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ProducerConfiguration(
    private val properties: OrderIndexerProperties,
    private val applicationEnvironmentInfo: ApplicationEnvironmentInfo
) {
    @Bean
    fun raribleProducerFactory(): ProducerFactory {
        return ProducerFactory(
            kafkaReplicaSet = properties.kafkaReplicaSet,
            blockchain = properties.blockchain,
            environment = applicationEnvironmentInfo.name
        )
    }

    @Bean
    fun protocolOrderPublisher(producerFactory: ProducerFactory): ProtocolOrderPublisher {
        return ProtocolOrderPublisher(
            orderActivityProducer = producerFactory.createOrderActivitiesProducer(),
            orderEventProducer = producerFactory.createOrderEventsProducer(),
            ordersPriceUpdateEventProducer = producerFactory.createOrderPriceUpdateEventsProducer(),
            globalOrderEventProducer = producerFactory.createGlobalOrderEventsProducer(),
            publishProperties = properties.publish
        )
    }
}
