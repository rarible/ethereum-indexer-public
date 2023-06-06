package com.rarible.protocol.erc20.core.configuration

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.kafka.json.JsonSerializer
import com.rarible.protocol.dto.Erc20BalanceEventDto
import com.rarible.protocol.dto.Erc20BalanceEventTopicProvider
import com.rarible.protocol.erc20.core.event.Erc20EventPublisher
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(Erc20IndexerProperties::class)
class ProducerConfiguration(
    private val properties: Erc20IndexerProperties,
    applicationEnvironmentInfo: ApplicationEnvironmentInfo
) {

    private val blockchain = properties.blockchain
    private val env = applicationEnvironmentInfo.name

    @Bean
    fun erc20EventPublisher(): Erc20EventPublisher {
        val producer = RaribleKafkaProducer(
            clientId = "$env.${blockchain.value}.protocol-erc20-events-importer",
            valueSerializerClass = JsonSerializer::class.java,
            valueClass = Erc20BalanceEventDto::class.java,
            defaultTopic = Erc20BalanceEventTopicProvider.getTopic(env, blockchain.value),
            bootstrapServers = properties.kafkaReplicaSet
        )
        return Erc20EventPublisher(producer)
    }
}
