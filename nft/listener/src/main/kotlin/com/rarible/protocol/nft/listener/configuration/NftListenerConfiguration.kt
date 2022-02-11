package com.rarible.protocol.nft.listener.configuration

import com.github.cloudyrock.spring.v5.EnableMongock
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.cache.EnableRaribleCache
import com.rarible.core.daemon.sequential.ConsumerBatchWorker
import com.rarible.core.daemon.sequential.ConsumerWorker
import com.rarible.core.daemon.sequential.ConsumerWorkerHolder
import com.rarible.core.lockredis.EnableRaribleRedisLock
import com.rarible.ethereum.converters.EnableScaletherMongoConversions
import com.rarible.protocol.dto.NftCollectionEventDto
import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.configuration.ProducerConfiguration
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ReduceSkipTokens
import com.rarible.protocol.nft.core.service.item.meta.BatchInternalItemHandler
import com.rarible.protocol.nft.core.service.item.meta.InternalItemHandler
import com.rarible.protocol.nft.core.service.token.meta.InternalCollectionHandler
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
@EnableConfigurationProperties(NftListenerProperties::class)
class NftListenerConfiguration(
    private val nftIndexerProperties: NftIndexerProperties,
    private val nftListenerProperties: NftListenerProperties,
    private val meterRegistry: MeterRegistry,
    private val applicationEnvironmentInfo: ApplicationEnvironmentInfo
) {
    private val logger = LoggerFactory.getLogger(ProducerConfiguration::class.java)

    @Bean
    fun reduceSkipTokens(): ReduceSkipTokens {
        return ReduceSkipTokens(nftListenerProperties.skipReduceTokens.map { ItemId.parseId(it) })
    }

    @Bean
    fun itemMetaExtenderWorker(
        internalItemHandler: InternalItemHandler,
        batchInternalItemHandler: BatchInternalItemHandler
    ): ConsumerWorkerHolder<NftItemEventDto> {
        logger.info("Creating batch of ${nftIndexerProperties.nftItemMetaExtenderWorkersCount} item meta extender workers")

        val workers = (1..nftIndexerProperties.nftItemMetaExtenderWorkersCount).map {
            val consumer = InternalItemHandler.createInternalItemConsumer(
                applicationEnvironmentInfo,
                nftIndexerProperties.blockchain,
                nftIndexerProperties.kafkaReplicaSet
            )
            val properties = nftIndexerProperties.daemonWorkerProperties
            val workerName = "nftItemMetaExtender.$it"

            if (nftIndexerProperties.featureFlags.internalMetaTopicBatchHandle) {
                logger.info("Activated internal batch handle, batch size handle ${nftIndexerProperties.featureFlags.internalMetaTopicBatchSize}")
                ConsumerBatchWorker(
                    consumer = consumer,
                    properties = properties,
                    eventHandler = batchInternalItemHandler,
                    meterRegistry = meterRegistry,
                    workerName = workerName
                )
            } else {
                ConsumerWorker(
                    consumer = consumer,
                    properties = properties,
                    eventHandler = internalItemHandler,
                    meterRegistry = meterRegistry,
                    workerName = workerName
                )
            }
        }
        return ConsumerWorkerHolder(workers)
    }

    @Bean
    fun collectionMetaExtenderWorker(internalCollectionHandler: InternalCollectionHandler): ConsumerWorkerHolder<NftCollectionEventDto> {
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
        return ConsumerWorkerHolder(workers)
    }

    @Bean
    fun itemMetaExtenderWorkerStarter(itemMetaExtenderWorker: ConsumerWorkerHolder<NftItemEventDto>): CommandLineRunner =
        CommandLineRunner { itemMetaExtenderWorker.start() }

    @Bean
    fun collectionMetaExtenderWorkerStarter(collectionMetaExtenderWorker: ConsumerWorkerHolder<NftCollectionEventDto>): CommandLineRunner =
        CommandLineRunner { collectionMetaExtenderWorker.start() }
}
