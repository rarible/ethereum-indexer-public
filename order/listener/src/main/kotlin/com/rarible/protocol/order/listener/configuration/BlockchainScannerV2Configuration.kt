package com.rarible.protocol.order.listener.configuration

import com.rarible.blockchain.scanner.configuration.KafkaProperties
import com.rarible.blockchain.scanner.ethereum.EnableEthereumScanner
import com.rarible.blockchain.scanner.ethereum.configuration.EthereumScannerProperties
import com.rarible.blockchain.scanner.ethereum.consumer.KafkaEntityEventConsumer
import com.rarible.blockchain.scanner.ethereum.consumer.factory.ConsumerWorkerFactory
import com.rarible.blockchain.scanner.ethereum.consumer.factory.DefaultConsumerWorkerFactory
import com.rarible.blockchain.scanner.ethereum.reduce.EntityEventListener
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.ethereum.listener.log.LogEventDescriptor
import com.rarible.ethereum.listener.log.LogEventDescriptorHolder
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableOnScannerV2
@EnableEthereumScanner
class BlockchainScannerV2Configuration(
    private val commonProperties: OrderIndexerProperties,
    private val listenerProperties: OrderListenerProperties,
    private val ethereumScannerProperties: EthereumScannerProperties,
    private val meterRegistry: MeterRegistry,
    private val applicationEnvironmentInfo: ApplicationEnvironmentInfo
) {
    @Bean
    fun consumerWorkerFactory(): ConsumerWorkerFactory {
        return DefaultConsumerWorkerFactory(
            properties = KafkaProperties(
                brokerReplicaSet = commonProperties.kafkaReplicaSet,
            ),
            daemonProperties = listenerProperties.eventConsumerWorker,
            meterRegistry = meterRegistry,
            ignoreContracts = emptySet(),
            host = applicationEnvironmentInfo.host,
            environment = applicationEnvironmentInfo.name,
            blockchain = commonProperties.blockchain.value,
            service = ethereumScannerProperties.service,
            workerCount = listenerProperties.logConsumeWorkerCount
        )
    }

    @Bean
    fun entityEventConsumer(
        entityEventListener: List<EntityEventListener>,
        consumerWorkerFactory: ConsumerWorkerFactory
    ): KafkaEntityEventConsumer {
        return KafkaEntityEventConsumer(consumerWorkerFactory).apply { start(entityEventListener) }
    }

    //TODO: Remove this workaround after full migrate to blockchain scanner v2
    @Bean
    @ConditionalOnMissingBean(LogEventDescriptorHolder::class)
    fun logEventDescriptorHolder(list: List<LogEventDescriptor<*>>): LogEventDescriptorHolder {
        return LogEventDescriptorHolder(list)
    }
}
