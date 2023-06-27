package com.rarible.protocol.erc20.listener.configuration

import com.rarible.blockchain.scanner.configuration.KafkaProperties
import com.rarible.blockchain.scanner.ethereum.EnableEthereumScanner
import com.rarible.blockchain.scanner.ethereum.configuration.EthereumScannerProperties
import com.rarible.blockchain.scanner.ethereum.reduce.EntityEventListener
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.protocol.erc20.core.configuration.Erc20IndexerProperties
import com.rarible.protocol.erc20.listener.consumer.KafkaEntityEventConsumer
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import scalether.domain.Address

@Configuration
@EnableEthereumScanner
class BlockchainScannerConfiguration(
    private val erc20IndexerProperties: Erc20IndexerProperties,
    private val erc20ListenerProperties: Erc20ListenerProperties,
    private val ethereumScannerProperties: EthereumScannerProperties,
    private val applicationEnvironmentInfo: ApplicationEnvironmentInfo
) {

    private val logger = LoggerFactory.getLogger(BlockchainScannerConfiguration::class.java)

    @Bean
    @ConditionalOnMissingBean(KafkaEntityEventConsumer::class)
    fun entityEventConsumer(
        entityEventListener: List<EntityEventListener>
    ): KafkaEntityEventConsumer {
        logger.info("Creating KafkaEntityEventConsumer with config: $erc20IndexerProperties $erc20ListenerProperties")
        return KafkaEntityEventConsumer(
            properties = KafkaProperties(
                brokerReplicaSet = erc20IndexerProperties.kafkaReplicaSet,
            ),
            host = applicationEnvironmentInfo.host,
            environment = applicationEnvironmentInfo.name,
            blockchain = erc20IndexerProperties.blockchain.value,
            service = ethereumScannerProperties.service,
            workerCount = erc20ListenerProperties.logConsumeWorkerCount,
            batchSize = erc20ListenerProperties.logConsumeWorkerBatchSize,
            ignoreContracts = erc20ListenerProperties.skipTransferContracts.map { Address.apply(it) }.toSet(),
        ).apply { start(entityEventListener) }
    }
}
