package com.rarible.protocol.nft.listener.configuration

import com.github.cloudyrock.spring.v5.EnableMongock
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.cache.EnableRaribleCache
import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.daemon.job.JobDaemonWorker
import com.rarible.core.daemon.sequential.ConsumerBatchWorker
import com.rarible.core.daemon.sequential.ConsumerWorker
import com.rarible.core.daemon.sequential.ConsumerWorkerHolder
import com.rarible.core.lockredis.EnableRaribleRedisLock
import com.rarible.ethereum.converters.EnableScaletherMongoConversions
import com.rarible.protocol.dto.NftCollectionEventDto
import com.rarible.protocol.dto.NftOwnershipEventDto
import com.rarible.protocol.nft.core.metric.CheckerMetrics
import com.rarible.protocol.nft.api.subscriber.NftIndexerEventsConsumerFactory
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.configuration.ProducerConfiguration
import com.rarible.protocol.nft.core.misc.RateLimiter
import com.rarible.protocol.nft.core.model.ActionEvent
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ReduceSkipTokens
import com.rarible.protocol.nft.core.producer.InternalTopicProvider
import com.rarible.protocol.nft.core.repository.token.TokenRepository
import com.rarible.protocol.nft.core.service.action.ActionEventHandler
import com.rarible.protocol.nft.core.service.action.ActionJobHandler
import com.rarible.protocol.nft.core.service.token.meta.InternalCollectionHandler
import com.rarible.protocol.nft.listener.service.checker.OwnershipBatchCheckerHandler
import com.rarible.protocol.nft.listener.service.item.InconsistentItemsRepairJobHandler
import com.rarible.protocol.nft.listener.service.item.ItemOwnershipConsistencyJobHandler
import com.rarible.protocol.nft.listener.service.ownership.OwnershipItemConsistencyJobHandler
import com.rarible.protocol.nft.listener.service.suspicios.UpdateSuspiciousItemsHandler
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import scalether.core.MonoEthereum
import java.time.Duration

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
    private val applicationEnvironmentInfo: ApplicationEnvironmentInfo,
    private val nftIndexerEventsConsumerFactory: NftIndexerEventsConsumerFactory
) {
    private val logger = LoggerFactory.getLogger(ProducerConfiguration::class.java)

    private val ownershipCheckerConsumerGroup =
        "${applicationEnvironmentInfo.name}.protocol.${nftIndexerProperties.blockchain.value}.nft.indexer.ownership"

    @Bean
    fun reduceSkipTokens(): ReduceSkipTokens {
        return ReduceSkipTokens(nftListenerProperties.skipReduceTokens.map { ItemId.parseId(it) })
    }

    @Bean
    fun updateSuspiciousItemsHandlerProperties(): UpdateSuspiciousItemsHandlerProperties {
        return nftListenerProperties.updateSuspiciousItemsHandler
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
        name = ["action-execute.enabled"],
        havingValue = "true"
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
        name = ["action-execute.enabled"],
        havingValue = "true"
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
        name = ["action-execute.enabled"],
        havingValue = "true"
    )
    fun actionConsumerWorkerStarter(actionConsumerWorker: ConsumerWorkerHolder<ActionEvent>): CommandLineRunner {
        return CommandLineRunner {
            actionConsumerWorker.start()
        }
    }

    @Bean
    @ConditionalOnProperty(name = ["listener.job.item-ownership-consistency.enabled"], havingValue = "true")
    fun itemOwnershipConsistencyWorker(handler: ItemOwnershipConsistencyJobHandler): JobDaemonWorker {
        return JobDaemonWorker(
            jobHandler = handler,
            meterRegistry = meterRegistry,
            properties = nftListenerProperties.itemOwnershipConsistency.daemon,
            workerName = "item-ownership-consistency-worker"
        ).apply { start() }
    }

    @Bean
    @ConditionalOnProperty(name = ["listener.job.ownership-item-consistency.enabled"], havingValue = "true")
    fun ownershipItemConsistencyWorker(handler: OwnershipItemConsistencyJobHandler): JobDaemonWorker {
        return JobDaemonWorker(
            jobHandler = handler,
            meterRegistry = meterRegistry,
            properties = nftListenerProperties.ownershipItemConsistency.daemon,
            workerName = "ownership-item-consistency-worker"
        ).apply { start() }
    }

    @Bean
    @ConditionalOnProperty(name = ["listener.job.inconsistent-items-repair.enabled"], havingValue = "true")
    fun inconsistentItemsRepairWorker(handler: InconsistentItemsRepairJobHandler): JobDaemonWorker {
        return JobDaemonWorker(
            jobHandler = handler,
            meterRegistry = meterRegistry,
            properties = nftListenerProperties.inconsistentItemsRepair.daemon,
            workerName = "inconsistent-items-repair-worker"
        ).apply { start() }
    }

    @Bean("inconsistentItemsRepairRateLimiter")
    fun inconsistentItemsRepairRateLimiter(): RateLimiter {
        return RateLimiter(
            nftListenerProperties.inconsistentItemsRepair.rateLimitMaxEntities,
            nftListenerProperties.inconsistentItemsRepair.rateLimitPeriod,
            "inconsistent-items-repair"
        )
    }

    @Bean
    @ConditionalOnProperty(name = ["listener.updateSuspiciousItemsHandler.enabled"], havingValue = "true")
    fun updateSuspiciousItemsHandlerWorker(
        handler: UpdateSuspiciousItemsHandler
    ): JobDaemonWorker {
        return JobDaemonWorker(
            jobHandler = handler,
            meterRegistry = meterRegistry,
            properties = DaemonWorkerProperties(
                pollingPeriod = Duration.ZERO,
            ),
            workerName = "update-suspicious-items-handler-worker"
        ).apply { start() }
    }

    @Bean
    fun checkerMetrics(meterRegistry: MeterRegistry): CheckerMetrics {
        return CheckerMetrics(nftIndexerProperties.blockchain, meterRegistry)
    }

    @Bean
    fun ownershipCheckerWorker(
        ethereum: MonoEthereum,
        tokenRepository: TokenRepository,
        checkerMetrics: CheckerMetrics
    ): ConsumerBatchWorker<NftOwnershipEventDto> {
        val consumer = nftIndexerEventsConsumerFactory.createOwnershipEventsConsumer(
            consumerGroup = ownershipCheckerConsumerGroup,
            blockchain = nftIndexerProperties.blockchain
        )
        return ConsumerBatchWorker(
            consumer = consumer,
            properties = nftListenerProperties.eventConsumerWorker,
            eventHandler = OwnershipBatchCheckerHandler(nftListenerProperties, ethereum, tokenRepository, checkerMetrics),
            meterRegistry = meterRegistry,
            workerName = "ownership-checker"
        ).apply { start() }
    }
}
