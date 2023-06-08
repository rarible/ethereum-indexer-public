package com.rarible.protocol.erc20.listener.configuration

import com.github.cloudyrock.spring.v5.EnableMongock
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.daemon.sequential.ConsumerBatchWorker
import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.ethereum.contract.EnableContractService
import com.rarible.ethereum.converters.EnableScaletherMongoConversions
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.dto.Erc20BalanceEventDto
import com.rarible.protocol.erc20.api.subscriber.Erc20IndexerEventsConsumerFactory
import com.rarible.protocol.erc20.core.configuration.ProducerConfiguration
import com.rarible.protocol.erc20.core.event.Erc20EventPublisher
import com.rarible.protocol.erc20.core.event.KafkaErc20BalanceEventListener
import com.rarible.protocol.erc20.core.metric.CheckerMetrics
import com.rarible.protocol.erc20.core.metric.DescriptorMetrics
import com.rarible.protocol.erc20.listener.scanner.BalanceBatchCheckerHandler
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import scalether.core.MonoEthereum

@EnableMongock
@Configuration
@EnableContractService
@EnableScaletherMongoConversions
@EnableConfigurationProperties(Erc20ListenerProperties::class)
@Import(ProducerConfiguration::class)
class Erc20ListenerConfiguration(
    environmentInfo: ApplicationEnvironmentInfo,
    private val commonProperties: Erc20ListenerProperties,
    private val meterRegistry: MeterRegistry,
    private val erc20IndexerEventsConsumerFactory: Erc20IndexerEventsConsumerFactory
) {

    private val erc20BalanceConsumerGroup =
        "${environmentInfo.name}.protocol.${commonProperties.blockchain.value}.erc20.indexer.erc20-balance"

    @Bean
    fun blockchain(): Blockchain {
        return commonProperties.blockchain
    }

    @Bean
    fun kafkaErc20BalanceEventListener(
        protocolEventPublisher: Erc20EventPublisher
    ): KafkaErc20BalanceEventListener {
        return KafkaErc20BalanceEventListener(protocolEventPublisher)
    }

    @Bean
    fun erc20BalanceCleanupJobProperties() = commonProperties.job.balanceCleanup

    @Bean
    fun checkerMetrics(blockchain: Blockchain, meterRegistry: MeterRegistry): CheckerMetrics {
        return CheckerMetrics(blockchain, meterRegistry)
    }

    @Bean
    fun descriptorMetrics(blockchain: Blockchain, meterRegistry: MeterRegistry): DescriptorMetrics {
        return DescriptorMetrics(blockchain, meterRegistry)
    }

    @Bean
    fun erc20BalanceCheckerWorker(
        ethereum: MonoEthereum,
        checkerMetrics: CheckerMetrics
    ): ConsumerBatchWorker<Erc20BalanceEventDto> {
        val args = erc20IndexerEventsConsumerFactory.createErc20BalanceEventsConsumer(
            consumerGroup = erc20BalanceConsumerGroup,
            blockchain = blockchain()
        )
        val consumer = RaribleKafkaConsumer<Erc20BalanceEventDto>(
            clientId = args.clientId,
            consumerGroup = args.consumerGroup,
            valueDeserializerClass = args.valueDeserializerClass,
            defaultTopic = args.defaultTopic,
            bootstrapServers = args.bootstrapServers,
            offsetResetStrategy = args.offsetResetStrategy
        )
        return ConsumerBatchWorker(
            consumer = consumer,
            properties = commonProperties.eventConsumerWorker,
            eventHandler = BalanceBatchCheckerHandler(ethereum, checkerMetrics, commonProperties),
            meterRegistry = meterRegistry,
            workerName = "erc20-balance-checker"
        ).apply { start() }
    }
}
