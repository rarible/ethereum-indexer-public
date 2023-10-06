package com.rarible.protocol.order.listener.configuration

import com.rarible.blockchain.scanner.configuration.KafkaProperties
import com.rarible.blockchain.scanner.consumer.LogRecordConsumerWorkerFactory
import com.rarible.blockchain.scanner.consumer.kafka.KafkaLogRecordConsumerWorkerFactory
import com.rarible.blockchain.scanner.consumer.kafka.KafkaLogRecordEventConsumer
import com.rarible.blockchain.scanner.ethereum.EnableEthereumScanner
import com.rarible.blockchain.scanner.ethereum.configuration.EthereumScannerProperties
import com.rarible.blockchain.scanner.ethereum.consumer.EthereumLogRecordMapper
import com.rarible.blockchain.scanner.ethereum.model.EthereumLogRecordEvent
import com.rarible.blockchain.scanner.framework.listener.LogRecordEventListener
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.ethereum.listener.log.LogEventDescriptor
import com.rarible.ethereum.listener.log.LogEventDescriptorHolder
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableEthereumScanner
class BlockchainScannerV2Configuration(
    private val commonProperties: OrderIndexerProperties,
    private val listenerProperties: OrderListenerProperties,
    private val ethereumScannerProperties: EthereumScannerProperties,
    private val applicationEnvironmentInfo: ApplicationEnvironmentInfo
) {
    @Bean
    fun consumerWorkerFactory(): LogRecordConsumerWorkerFactory {
        return KafkaLogRecordConsumerWorkerFactory(
            properties = KafkaProperties(
                brokerReplicaSet = commonProperties.kafkaReplicaSet,
            ),
            daemonProperties = listenerProperties.eventConsumerWorker,
            host = applicationEnvironmentInfo.host,
            environment = applicationEnvironmentInfo.name,
            blockchain = commonProperties.blockchain.value,
            service = ethereumScannerProperties.service,
        )
    }

    @Bean
    @ConditionalOnMissingBean(KafkaLogRecordConsumerWorkerFactory::class)
    fun entityEventConsumer(
        logRecordEventListeners: List<LogRecordEventListener>,
        consumerWorkerFactory: LogRecordConsumerWorkerFactory
    ): KafkaLogRecordEventConsumer {
        return KafkaLogRecordEventConsumer(consumerWorkerFactory).apply {
            start(
                logRecordListeners = logRecordEventListeners,
                logRecordType = EthereumLogRecordEvent::class.java,
                logRecordMapper = EthereumLogRecordMapper,
                logRecordFilters = emptyList(),
                workerCount = listenerProperties.logConsumeWorkerCount,
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
