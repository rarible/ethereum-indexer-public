package com.rarible.protocol.nft.listener.configuration

import com.rarible.blockchain.scanner.configuration.KafkaProperties
import com.rarible.blockchain.scanner.ethereum.EnableEthereumScanner
import com.rarible.blockchain.scanner.reconciliation.DefaultReconciliationFormProvider
import com.rarible.blockchain.scanner.reconciliation.ReconciliationFromProvider
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.application.ApplicationInfo
import com.rarible.ethereum.listener.log.EnableLogListeners
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.service.EntityEventListener
import com.rarible.protocol.nft.listener.NftListenerApplication
import com.rarible.protocol.nft.listener.consumer.KafkaEntityEventConsumer
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@FlowPreview
@ExperimentalCoroutinesApi
@Configuration
@ConditionalOnProperty(name = ["common.feature-flags.scanner-version"], havingValue = "V2")
@EnableLogListeners(scanPackage = [NftListenerApplication::class])
@EnableEthereumScanner
class BlockchainScannerV2Configuration(
    private val nftIndexerProperties: NftIndexerProperties,
    private val nftListenerProperties: NftListenerProperties,
    private val meterRegistry: MeterRegistry,
    private val applicationEnvironmentInfo: ApplicationEnvironmentInfo,
    private val applicationInfo: ApplicationInfo
) {
    @Bean
    fun reconciliationFromProvider(): ReconciliationFromProvider {
        return DefaultReconciliationFormProvider()
    }

    @Bean
    fun entityEventConsumer(
        entityEventListener: List<EntityEventListener>
    ): KafkaEntityEventConsumer {
        return KafkaEntityEventConsumer(
            properties = KafkaProperties(
                brokerReplicaSet = nftIndexerProperties.kafkaReplicaSet,
                enabled = true,
                maxPollRecords = nftIndexerProperties.maxPollRecords
            ),
            daemonProperties = nftListenerProperties.eventConsumerWorker,
            meterRegistry = meterRegistry,
            host = applicationEnvironmentInfo.host,
            environment = applicationEnvironmentInfo.name,
            blockchain = nftIndexerProperties.blockchain.value,
            service = applicationInfo.serviceName
        ).apply { start(entityEventListener.associateBy { it.groupId }) }
    }
}
