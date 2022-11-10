package com.rarible.protocol.erc20.listener.configuration

import com.rarible.blockchain.scanner.configuration.KafkaProperties
import com.rarible.blockchain.scanner.ethereum.EnableEthereumScanner
import com.rarible.blockchain.scanner.ethereum.configuration.EthereumScannerProperties
import com.rarible.blockchain.scanner.ethereum.reduce.EntityEventListener
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.ethereum.listener.log.LogEventDescriptor
import com.rarible.ethereum.listener.log.LogEventDescriptorHolder
import com.rarible.protocol.erc20.core.configuration.Erc20IndexerProperties
import com.rarible.protocol.erc20.listener.consumer.KafkaEntityEventConsumer
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import scalether.domain.Address

@Configuration
@EnableOnScannerV2
@EnableEthereumScanner
class BlockchainScannerV2Configuration(
    private val erc20IndexerProperties: Erc20IndexerProperties,
    private val erc20ListenerProperties: Erc20ListenerProperties,
    private val ethereumScannerProperties: EthereumScannerProperties,
    private val meterRegistry: MeterRegistry,
    private val applicationEnvironmentInfo: ApplicationEnvironmentInfo
) {
    private val logger = LoggerFactory.getLogger(BlockchainScannerV2Configuration::class.java)

    @Bean
    fun entityEventConsumer(
        entityEventListener: List<EntityEventListener>
    ): KafkaEntityEventConsumer {
        logger.info("Creating KafkaEntityEventConsumer with config: $erc20IndexerProperties $erc20ListenerProperties")
        return KafkaEntityEventConsumer(
            properties = KafkaProperties(
                brokerReplicaSet = erc20IndexerProperties.kafkaReplicaSet,
            ),
            daemonProperties = erc20ListenerProperties.eventConsumerWorker,
            meterRegistry = meterRegistry,
            host = applicationEnvironmentInfo.host,
            environment = applicationEnvironmentInfo.name,
            blockchain = erc20IndexerProperties.blockchain.value,
            service = ethereumScannerProperties.service,
            workerCount = erc20ListenerProperties.logConsumeWorkerCount,
            ignoreContracts = erc20ListenerProperties.skipTransferContracts.map { Address.apply(it) }.toSet(),
        ).apply { start(entityEventListener) }
    }

    //TODO: Remove this workaround after full migrate to blockchain scanner v2
    @Bean
    @ConditionalOnMissingBean(LogEventDescriptorHolder::class)
    fun logEventDescriptorHolder(list: List<LogEventDescriptor<*>>): LogEventDescriptorHolder {
        return LogEventDescriptorHolder(list)
    }
}
