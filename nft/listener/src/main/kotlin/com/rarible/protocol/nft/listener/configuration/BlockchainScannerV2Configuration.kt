package com.rarible.protocol.nft.listener.configuration

import com.rarible.blockchain.scanner.configuration.KafkaProperties
import com.rarible.blockchain.scanner.consumer.TransactionRecordConsumerWorkerFactory
import com.rarible.blockchain.scanner.consumer.TransactionRecordMapper
import com.rarible.blockchain.scanner.consumer.kafka.KafkaTransactionRecordConsumerWorkerFactory
import com.rarible.blockchain.scanner.consumer.kafka.KafkaTransactionRecordEventConsumer
import com.rarible.blockchain.scanner.ethereum.EnableEthereumScanner
import com.rarible.blockchain.scanner.ethereum.configuration.EthereumScannerProperties
import com.rarible.blockchain.scanner.ethereum.reduce.EntityEventListener
import com.rarible.blockchain.scanner.framework.data.TransactionRecordEvent
import com.rarible.blockchain.scanner.framework.listener.TransactionRecordEventListener
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.ethereum.listener.log.LogEventDescriptor
import com.rarible.ethereum.listener.log.LogEventDescriptorHolder
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.listener.consumer.KafkaEntityEventConsumer
import com.rarible.protocol.nft.listener.service.resolver.IgnoredTokenResolver
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableOnScannerV2
@EnableEthereumScanner
class BlockchainScannerV2Configuration(
    private val nftIndexerProperties: NftIndexerProperties,
    private val nftListenerProperties: NftListenerProperties,
    private val ethereumScannerProperties: EthereumScannerProperties,
    private val meterRegistry: MeterRegistry,
    private val applicationEnvironmentInfo: ApplicationEnvironmentInfo,
    private val ignoredTokenResolver: IgnoredTokenResolver,
) {
    private val logger = LoggerFactory.getLogger(BlockchainScannerV2Configuration::class.java)

    @Bean
    @ConditionalOnMissingBean(KafkaEntityEventConsumer::class)
    fun entityEventConsumer(
        entityEventListener: List<EntityEventListener>
    ): KafkaEntityEventConsumer {
        logger.info("Creating KafkaEntityEventConsumer with config: $nftIndexerProperties $nftListenerProperties")
        return KafkaEntityEventConsumer(
            properties = KafkaProperties(
                brokerReplicaSet = nftIndexerProperties.kafkaReplicaSet,
            ),
            host = applicationEnvironmentInfo.host,
            environment = applicationEnvironmentInfo.name,
            blockchain = nftIndexerProperties.blockchain.value,
            service = ethereumScannerProperties.service,
            workerCount = nftListenerProperties.logConsumeWorkerCount,
            batchSize = nftListenerProperties.logConsumeWorkerBatchSize,
            ignoreContracts = ignoredTokenResolver.resolve(),
        ).apply { start(entityEventListener) }
    }

    @Bean
    fun transactionRecordConsumerWorkerFactory(): TransactionRecordConsumerWorkerFactory {
        return KafkaTransactionRecordConsumerWorkerFactory(
            properties = KafkaProperties(
                brokerReplicaSet = nftIndexerProperties.kafkaReplicaSet,
            ),
            daemonProperties = nftListenerProperties.eventConsumerWorker,
            host = applicationEnvironmentInfo.host,
            environment = applicationEnvironmentInfo.name,
            blockchain = nftIndexerProperties.blockchain.value,
            service = ethereumScannerProperties.service,
        )
    }

    @Bean
    fun transactionEntityEventConsumer(
        transactionRecordEventListeners: List<TransactionRecordEventListener>,
        consumerWorkerFactory: TransactionRecordConsumerWorkerFactory
    ): KafkaTransactionRecordEventConsumer {
        return KafkaTransactionRecordEventConsumer(consumerWorkerFactory).apply {
            start(
                transactionRecordListeners = transactionRecordEventListeners,
                transactionRecordType = TransactionRecordEvent::class.java,
                transactionRecordMapper = object : TransactionRecordMapper<TransactionRecordEvent> {
                    override fun map(event: TransactionRecordEvent): TransactionRecordEvent = event
                },
                transactionRecordFilters = emptyList(),
                workerCount = nftListenerProperties.logConsumeWorkerCount,
            )
        }
    }

    // TODO: Remove this workaround after full migrate to blockchain scanner v2
    @Bean
    @ConditionalOnMissingBean(LogEventDescriptorHolder::class)
    fun logEventDescriptorHolder(list: List<LogEventDescriptor<*>>): LogEventDescriptorHolder {
        return LogEventDescriptorHolder(list)
    }
}
