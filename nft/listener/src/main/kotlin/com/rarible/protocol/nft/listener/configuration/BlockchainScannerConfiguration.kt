package com.rarible.protocol.nft.listener.configuration

import com.rarible.blockchain.scanner.configuration.KafkaProperties
import com.rarible.blockchain.scanner.consumer.LogRecordConsumerWorkerFactory
import com.rarible.blockchain.scanner.consumer.LogRecordFilter
import com.rarible.blockchain.scanner.consumer.LogRecordMapper
import com.rarible.blockchain.scanner.consumer.TransactionRecordConsumerWorkerFactory
import com.rarible.blockchain.scanner.consumer.TransactionRecordMapper
import com.rarible.blockchain.scanner.consumer.kafka.KafkaLogRecordConsumerWorkerFactory
import com.rarible.blockchain.scanner.consumer.kafka.KafkaLogRecordEventConsumer
import com.rarible.blockchain.scanner.consumer.kafka.KafkaTransactionRecordConsumerWorkerFactory
import com.rarible.blockchain.scanner.consumer.kafka.KafkaTransactionRecordEventConsumer
import com.rarible.blockchain.scanner.ethereum.EnableEthereumScanner
import com.rarible.blockchain.scanner.ethereum.configuration.EthereumScannerProperties
import com.rarible.blockchain.scanner.ethereum.model.EthereumLogRecordEvent
import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.blockchain.scanner.framework.data.TransactionRecordEvent
import com.rarible.blockchain.scanner.framework.listener.LogRecordEventListener
import com.rarible.blockchain.scanner.framework.listener.TransactionRecordEventListener
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.ethereum.listener.log.LogEventDescriptor
import com.rarible.ethereum.listener.log.LogEventDescriptorHolder
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableEthereumScanner
class BlockchainScannerConfiguration(
    private val nftIndexerProperties: NftIndexerProperties,
    private val nftListenerProperties: NftListenerProperties,
    private val ethereumScannerProperties: EthereumScannerProperties,
    private val applicationEnvironmentInfo: ApplicationEnvironmentInfo
) {

    @Bean
    fun logRecordConsumerWorkerFactory(): LogRecordConsumerWorkerFactory {
        return KafkaLogRecordConsumerWorkerFactory(
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
    fun logEntityEventConsumer(
        logRecordEventListeners: List<LogRecordEventListener>,
        logRecordEventFilters: List<LogRecordFilter<EthereumLogRecordEvent>>,
        consumerWorkerFactory: LogRecordConsumerWorkerFactory
    ): KafkaLogRecordEventConsumer {
        return KafkaLogRecordEventConsumer(consumerWorkerFactory).apply {
            start(
                logRecordListeners = logRecordEventListeners,
                logRecordType = EthereumLogRecordEvent::class.java,
                logRecordMapper = object : LogRecordMapper<EthereumLogRecordEvent> {
                    override fun map(event: EthereumLogRecordEvent): LogRecordEvent = LogRecordEvent(
                        record = event.record,
                        reverted = event.reverted,
                        eventTimeMarks = event.eventTimeMarks
                    )
                },
                logRecordFilters = logRecordEventFilters,
                workerCount = nftListenerProperties.logConsumeWorkerCount,
                coroutineThreadCount = nftListenerProperties.logConsumeCoroutineThreadCount
            )
        }
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
