package com.rarible.protocol.order.core.configuration

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.ethereum.monitoring.EventCountMetrics
import com.rarible.protocol.order.core.producer.ProducerFactory
import io.micrometer.core.instrument.MeterRegistry
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
            environment = applicationEnvironmentInfo.name,
            compression = properties.compression,
        )
    }

    @Bean
    fun orderActivityProducer(producerFactory: ProducerFactory) = producerFactory.createOrderActivitiesProducer()

    @Bean
    fun orderEventProducer(producerFactory: ProducerFactory) = producerFactory.createOrderEventsProducer()

    @Bean
    fun ordersPriceUpdateEventProducer(producerFactory: ProducerFactory) = producerFactory.createOrderPriceUpdateEventsProducer()

    @Bean
    fun auctionActivityProducer(producerFactory: ProducerFactory) = producerFactory.createAuctionActivitiesProducer()

    @Bean
    fun auctionEventProducer(producerFactory: ProducerFactory) = producerFactory.createAuctionEventsProducer()

    @Bean
    fun publishProperties() = properties.publish

    @Bean
    fun eventCountMetrics(registry: MeterRegistry): EventCountMetrics {
        return EventCountMetrics(registry)
    }
}
