package com.rarible.protocol.nft.listener.configuration

import com.github.cloudyrock.spring.v5.EnableMongock
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.cache.EnableRaribleCache
import com.rarible.core.daemon.job.JobDaemonWorker
import com.rarible.core.daemon.sequential.ConsumerWorker
import com.rarible.core.daemon.sequential.ConsumerWorkerHolder
import com.rarible.core.lockredis.EnableRaribleRedisLock
import com.rarible.ethereum.converters.EnableScaletherMongoConversions
import com.rarible.protocol.dto.NftCollectionEventDto
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.configuration.ProducerConfiguration
import com.rarible.protocol.nft.core.model.ActionEvent
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ReduceSkipTokens
import com.rarible.protocol.nft.core.producer.InternalTopicProvider
import com.rarible.protocol.nft.core.service.action.ActionEventHandler
import com.rarible.protocol.nft.core.service.token.meta.InternalCollectionHandler
import com.rarible.protocol.nft.core.service.action.ActionJobHandler
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
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
        return ConsumerWorkerHolder(workers).apply { start() }
    }

    @Bean
    @ConditionalOnProperty(
        prefix = RARIBLE_PROTOCOL_LISTENER_STORAGE,
        name=["action-execute.enabled"],
        havingValue="true"
    )
    fun actionConsumerWorker(internalActionHandler: ActionEventHandler): ConsumerWorkerHolder<ActionEvent> {
        logger.info("Creating batch of ${nftIndexerProperties.actionWorkersCount} action workers")
        val workers = (1..nftIndexerProperties.actionWorkersCount).map {
            ConsumerWorker(
                consumer = InternalTopicProvider.createInternalActionEventConsumer(
                    applicationEnvironmentInfo,
                    nftIndexerProperties.blockchain,
                    nftIndexerProperties.kafkaReplicaSet
                ),
                properties = nftIndexerProperties.daemonWorkerProperties,
                eventHandler = internalActionHandler,
                meterRegistry = meterRegistry,
                workerName = "ActionHandler.$it"
            )
        }
        return ConsumerWorkerHolder(workers)
    }

    @Bean
    @ConditionalOnProperty(
        prefix = RARIBLE_PROTOCOL_LISTENER_STORAGE,
        name=["action-execute.enabled"],
        havingValue="true"
    )
    fun actionExecutorWorker(handler: ActionJobHandler): JobDaemonWorker {
        return JobDaemonWorker(
            jobHandler = handler,
            meterRegistry = meterRegistry,
            properties = nftListenerProperties.actionExecute.daemon,
            workerName = "action-executor-worker"
        ).apply { start() }
    }

    @Bean
    @ConditionalOnProperty(
        prefix = RARIBLE_PROTOCOL_LISTENER_STORAGE,
        name=["action-execute.enabled"],
        havingValue="true"
    )
    fun actionConsumerWorkerStarter(actionConsumerWorker: ConsumerWorkerHolder<ActionEvent>): CommandLineRunner {
        return CommandLineRunner {
            actionConsumerWorker.start()
        }
    }
}
