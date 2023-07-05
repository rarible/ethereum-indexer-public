package com.rarible.protocol.erc20.core.configuration

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.kafka.json.JsonSerializer
import com.rarible.protocol.dto.Erc20BalanceEventDto
import com.rarible.protocol.dto.Erc20BalanceEventTopicProvider
import com.rarible.protocol.erc20.core.event.Erc20EventPublisher
import com.rarible.protocol.erc20.core.service.reduce.Erc20EventChainUpdateService
import com.rarible.protocol.erc20.core.service.reduce.Erc20EventReduceService
import com.rarible.protocol.erc20.core.service.reduce.Erc20EventService
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(Erc20IndexerProperties::class)
class ProducerConfiguration(
    private val properties: Erc20IndexerProperties,
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    private val applicationEnvironmentInfo: ApplicationEnvironmentInfo
) {

    private val blockchain = properties.blockchain
    private val env = applicationEnvironmentInfo.name

    @Bean
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    fun erc20EventService(
        erc20EventChainUpdateService: Erc20EventChainUpdateService,
        erc20EventReduceService: Erc20EventReduceService,
    ): Erc20EventService {
        val erc20EventListener = if (properties.featureFlags.chainBalanceUpdateEnabled)
            erc20EventChainUpdateService
        else erc20EventReduceService

        return Erc20EventService(
            erc20EventListener,
            properties
        )
    }

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
