package com.rarible.protocol.erc20.core.configuration

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.dto.Erc20BalanceEventDto
import com.rarible.protocol.erc20.core.producer.ProducerFactory
import com.rarible.protocol.erc20.core.producer.ProtocolEventPublisher
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(Erc20IndexerProperties::class)
class ProducerConfiguration(
    private val properties: Erc20IndexerProperties,
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
    fun erc20EventsRaribleKafkaProducer(
        producerFactory: ProducerFactory
    ): RaribleKafkaProducer<Erc20BalanceEventDto> {
        return producerFactory.createErc20EventsProducer()
    }

    @Bean
    fun protocolEventPublisher(producer: RaribleKafkaProducer<Erc20BalanceEventDto>): ProtocolEventPublisher {
        return ProtocolEventPublisher(producer)
    }
}
