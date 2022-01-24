package com.rarible.protocol.nft.listener.configuration

import com.rarible.blockchain.scanner.configuration.KafkaProperties
import com.rarible.blockchain.scanner.ethereum.EnableEthereumScanner
import com.rarible.blockchain.scanner.ethereum.configuration.EthereumScannerProperties
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.ethereum.listener.log.LogEventDescriptor
import com.rarible.ethereum.listener.log.LogEventDescriptorHolder
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.service.EntityEventListener
import com.rarible.protocol.nft.listener.consumer.KafkaEntityEventConsumer
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(name = ["common.feature-flags.scanner-version"], havingValue = "V2")
@EnableEthereumScanner
class BlockchainScannerV2Configuration(
    private val nftIndexerProperties: NftIndexerProperties,
    private val nftListenerProperties: NftListenerProperties,
    private val ethereumScannerProperties: EthereumScannerProperties,
    private val meterRegistry: MeterRegistry,
    private val applicationEnvironmentInfo: ApplicationEnvironmentInfo
) {
    @Bean
    fun entityEventConsumer(
        entityEventListener: List<EntityEventListener>
    ): KafkaEntityEventConsumer {
        return KafkaEntityEventConsumer(
            properties = KafkaProperties(
                brokerReplicaSet = nftIndexerProperties.kafkaReplicaSet,
            ),
            daemonProperties = nftListenerProperties.eventConsumerWorker,
            meterRegistry = meterRegistry,
            host = applicationEnvironmentInfo.host,
            environment = applicationEnvironmentInfo.name,
            blockchain = nftIndexerProperties.blockchain.value,
            service = ethereumScannerProperties.service,
            workerCount = nftListenerProperties.logConsumeWorkerCount
        ).apply { start(entityEventListener.associateBy { it.id }) }
    }

    //TODO: Remove this workaround after full migrate to blockchain scanner v2
    @Bean
    @ConditionalOnMissingBean(LogEventDescriptorHolder::class)
    fun logEventDescriptorHolder(list: List<LogEventDescriptor<*>>): LogEventDescriptorHolder {
        return LogEventDescriptorHolder(list)
    }
}
