package com.rarible.protocol.nft.listener.configuration

import com.github.cloudyrock.spring.v5.EnableMongock
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.cache.EnableRaribleCache
import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.daemon.job.JobDaemonWorker
import com.rarible.core.kafka.RaribleKafkaConsumerFactory
import com.rarible.core.kafka.RaribleKafkaConsumerSettings
import com.rarible.core.kafka.RaribleKafkaConsumerWorker
import com.rarible.core.lockredis.EnableRaribleRedisLock
import com.rarible.ethereum.converters.EnableScaletherMongoConversions
import com.rarible.protocol.dto.NftOwnershipEventDto
import com.rarible.protocol.dto.NftOwnershipEventTopicProvider
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.configuration.ProducerConfiguration
import com.rarible.protocol.nft.core.configuration.PropertiesCore
import com.rarible.protocol.nft.core.metric.CheckerMetrics
import com.rarible.protocol.nft.core.misc.RateLimiter
import com.rarible.protocol.nft.core.model.ActionEvent
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ReduceSkipTokens
import com.rarible.protocol.nft.core.producer.InternalTopicProvider
import com.rarible.protocol.nft.core.service.action.ActionEventHandler
import com.rarible.protocol.nft.core.service.action.ActionJobHandler
import com.rarible.protocol.nft.listener.service.checker.OwnershipBatchCheckerHandler
import com.rarible.protocol.nft.listener.service.item.InconsistentItemsRepairJobHandler
import com.rarible.protocol.nft.listener.service.item.ItemOwnershipConsistencyJobHandler
import com.rarible.protocol.nft.listener.service.ownership.OwnershipItemConsistencyJobHandler
import com.rarible.protocol.nft.listener.service.suspicios.UpdateSuspiciousItemsHandler
import io.micrometer.core.instrument.MeterRegistry
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@EnableMongock
@EnableRaribleRedisLock
@EnableRaribleCache
@Configuration
@EnableScaletherMongoConversions
@EnableConfigurationProperties(NftListenerProperties::class)
@PropertiesCore
class NftListenerConfiguration(
    private val nftIndexerProperties: NftIndexerProperties,
    private val nftListenerProperties: NftListenerProperties,
    private val meterRegistry: MeterRegistry,
    applicationEnvironmentInfo: ApplicationEnvironmentInfo
) {
    private val logger = LoggerFactory.getLogger(ProducerConfiguration::class.java)

    private val blockchain = nftIndexerProperties.blockchain
    private val env = applicationEnvironmentInfo.name
    private val host = applicationEnvironmentInfo.host

    @Bean
    fun reduceSkipTokens(): ReduceSkipTokens {
        return ReduceSkipTokens(nftListenerProperties.skipReduceTokens.map { ItemId.parseId(it) })
    }

    @Bean
    fun updateSuspiciousItemsHandlerProperties(): UpdateSuspiciousItemsHandlerProperties {
        return nftListenerProperties.updateSuspiciousItemsHandler
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
        return CheckerMetrics(blockchain, meterRegistry)
    }

    @Bean
    @ConditionalOnProperty(
        prefix = RARIBLE_PROTOCOL_LISTENER_STORAGE,
        name = ["action-execute.enabled"],
        havingValue = "true"
    )
    fun actionConsumerWorker(
        factory: RaribleKafkaConsumerFactory,
        handler: ActionEventHandler
    ): RaribleKafkaConsumerWorker<ActionEvent> {
        val settings = RaribleKafkaConsumerSettings(
            hosts = nftIndexerProperties.kafkaReplicaSet,
            group = "protocol.${blockchain.value}.nft.indexer.internal.action",
            topic = InternalTopicProvider.getItemActionTopic(env, blockchain.value),
            valueClass = ActionEvent::class.java,
            async = true,
            concurrency = nftIndexerProperties.actionWorkersCount,
            batchSize = 500,
            offsetResetStrategy = OffsetResetStrategy.LATEST
        )
        return factory.createWorker(settings, handler)
    }

    @Bean
    fun ownershipCheckerWorker(
        factory: RaribleKafkaConsumerFactory,
        handler: OwnershipBatchCheckerHandler
    ): RaribleKafkaConsumerWorker<NftOwnershipEventDto> {
        val settings = RaribleKafkaConsumerSettings(
            hosts = nftIndexerProperties.kafkaReplicaSet,
            group = "protocol.${blockchain.value}.nft.indexer.ownership",
            topic = NftOwnershipEventTopicProvider.getTopic(env, blockchain.value),
            valueClass = NftOwnershipEventDto::class.java,
            async = false,
            concurrency = 9,
            batchSize = 500,
            offsetResetStrategy = OffsetResetStrategy.LATEST
        )
        return factory.createWorker(settings, handler)
    }
}
