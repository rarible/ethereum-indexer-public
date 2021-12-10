package com.rarible.protocol.nft.listener.configuration

import com.github.cloudyrock.spring.v5.EnableMongock
import com.rarible.blockchain.scanner.configuration.KafkaProperties
import com.rarible.blockchain.scanner.reconciliation.DefaultReconciliationFormProvider
import com.rarible.blockchain.scanner.reconciliation.ReconciliationFromProvider
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.application.ApplicationInfo
import com.rarible.core.cache.EnableRaribleCache
import com.rarible.core.daemon.sequential.ConsumerWorker
import com.rarible.core.lockredis.EnableRaribleRedisLock
import com.rarible.ethereum.converters.EnableScaletherMongoConversions
import com.rarible.ethereum.listener.log.EnableLogListeners
import com.rarible.ethereum.listener.log.persist.BlockRepository
import com.rarible.ethereum.monitoring.BlockchainMonitoringWorker
import com.rarible.protocol.dto.NftCollectionEventDto
import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.configuration.ProducerConfiguration
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ReduceSkipTokens
import com.rarible.protocol.nft.core.producer.BatchedConsumerWorker
import com.rarible.protocol.nft.core.service.EntityEventListener
import com.rarible.protocol.nft.core.service.item.meta.InternalItemHandler
import com.rarible.protocol.nft.core.service.token.meta.InternalCollectionHandler
import com.rarible.protocol.nft.listener.NftListenerApplication
import com.rarible.protocol.nft.listener.consumer.KafkaEntityEventConsumer
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@EnableMongock
@EnableRaribleRedisLock
@EnableRaribleCache
@Configuration
@EnableScaletherMongoConversions
@EnableLogListeners(scanPackage = [NftListenerApplication::class])
@EnableConfigurationProperties(NftListenerProperties::class)
class NftListenerConfiguration(
    private val nftIndexerProperties: NftIndexerProperties,
    private val nftListenerProperties: NftListenerProperties,
    private val meterRegistry: MeterRegistry,
    private val blockRepository: BlockRepository,
    private val applicationEnvironmentInfo: ApplicationEnvironmentInfo,
    private val applicationInfo: ApplicationInfo
) {
    private val logger = LoggerFactory.getLogger(ProducerConfiguration::class.java)

    @Bean
    fun reconciliationFromProvider(): ReconciliationFromProvider {
        return DefaultReconciliationFormProvider()
    }

    @Bean
    fun reduceSkipTokens(): ReduceSkipTokens {
        return ReduceSkipTokens(nftListenerProperties.skipReduceTokens.map { ItemId.parseId(it) })
    }

    @Bean
    fun blockchainMonitoringWorker(): BlockchainMonitoringWorker {
        return BlockchainMonitoringWorker(
            properties = nftListenerProperties.monitoringWorker,
            blockchain = nftIndexerProperties.blockchain,
            meterRegistry = meterRegistry,
            blockRepository = blockRepository
        )
    }

    @Bean
    fun itemMetaExtenderWorker(internalItemHandler: InternalItemHandler): BatchedConsumerWorker<NftItemEventDto> {
        logger.info("Creating batch of ${nftIndexerProperties.nftItemMetaExtenderWorkersCount} item meta extender workers")
        val workers = (1..nftIndexerProperties.nftItemMetaExtenderWorkersCount).map {
            ConsumerWorker(
                consumer = InternalItemHandler.createInternalItemConsumer(
                    applicationEnvironmentInfo,
                    nftIndexerProperties.blockchain,
                    nftIndexerProperties.kafkaReplicaSet
                ),
                properties = nftIndexerProperties.daemonWorkerProperties,
                eventHandler = internalItemHandler,
                meterRegistry = meterRegistry,
                workerName = "nftItemMetaExtender.$it"
            )
        }
        return BatchedConsumerWorker(workers)
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

    @Bean
    fun collectionMetaExtenderWorker(internalCollectionHandler: InternalCollectionHandler): BatchedConsumerWorker<NftCollectionEventDto> {
        logger.info("Creating batch of ${nftIndexerProperties.nftCollectionMetaExtenderWorkersCount} collection meta extender workers")
        val workers = (1..nftIndexerProperties.nftCollectionMetaExtenderWorkersCount).map {
            ConsumerWorker(
                consumer = InternalCollectionHandler.createInternalCollectionConsumer(
                    applicationEnvironmentInfo,
                    nftIndexerProperties.blockchain,
                    nftIndexerProperties.kafkaReplicaSet
                ),
                properties = nftIndexerProperties.daemonWorkerProperties,
                eventHandler = internalCollectionHandler,
                meterRegistry = meterRegistry,
                workerName = "nftCollectionMetaExtender.$it"
            )
        }
        return BatchedConsumerWorker(workers)
    }

    @Bean
    fun itemMetaExtenderWorkerStarter(itemMetaExtenderWorker: BatchedConsumerWorker<NftItemEventDto>): CommandLineRunner =
        CommandLineRunner { itemMetaExtenderWorker.start() }

    @Bean
    fun collectionMetaExtenderWorkerStarter(collectionMetaExtenderWorker: BatchedConsumerWorker<NftItemEventDto>): CommandLineRunner =
        CommandLineRunner { collectionMetaExtenderWorker.start() }
}
