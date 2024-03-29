package com.rarible.protocol.nft.listener.configuration

import com.rarible.core.daemon.DaemonWorkerProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.time.Duration

internal const val RARIBLE_PROTOCOL_LISTENER_STORAGE = "listener"

@ConstructorBinding
@ConfigurationProperties(RARIBLE_PROTOCOL_LISTENER_STORAGE)
data class NftListenerProperties(
    val skipReduceTokens: List<String> = emptyList(),
    val skipContracts: List<String> = emptyList(),
    val skipTransferContracts: List<String> = emptyList(),
    val skipTokenOwnershipTransferred: Boolean = false,
    val logConsumeWorkerCount: Int = 9,
    val logConsumeCoroutineThreadCount: Int = 4,
    val logConsumeWorkerBatchSize: Int = 500,
    val monitoringWorker: DaemonWorkerProperties = DaemonWorkerProperties(),
    val eventConsumerWorker: DaemonWorkerProperties = DaemonWorkerProperties(),
    val enableCheckDataQualityJob: Boolean = false,
    var elementsFetchJobSize: Int = 1000,
    val itemOwnershipConsistency: ItemOwnershipConsistencyProperties = ItemOwnershipConsistencyProperties(),
    val ownershipItemConsistency: OwnershipItemConsistencyProperties = OwnershipItemConsistencyProperties(),
    val inconsistentItemsRepair: InconsistentItemsRepairProperties = InconsistentItemsRepairProperties(),
    val updateSuspiciousItemsHandler: UpdateSuspiciousItemsHandlerProperties = UpdateSuspiciousItemsHandlerProperties(),
    val ownershipCheckerProperties: OwnershipCheckerProperties = OwnershipCheckerProperties(),
    val fixStandardJob: FixStandardJobProperties = FixStandardJobProperties()
)

data class ItemOwnershipConsistencyProperties(
    val autofix: Boolean = true,
    val checkTimeOffset: Duration = Duration.ofSeconds(30),
    val parallelism: Int = 4,
    val daemon: DaemonWorkerProperties = DaemonWorkerProperties(
        pollingPeriod = Duration.ofMinutes(1),
        errorDelay = Duration.ofMinutes(1),
    ),
)

data class OwnershipItemConsistencyProperties(
    val autofix: Boolean = true,
    val checkTimeOffset: Duration = Duration.ofSeconds(30),
    val parallelism: Int = 16,
    val daemon: DaemonWorkerProperties = DaemonWorkerProperties(
        pollingPeriod = Duration.ofMinutes(1),
        errorDelay = Duration.ofMinutes(1),
    ),
)

data class InconsistentItemsRepairProperties(
    val parallelism: Int = 1,
    val daemon: DaemonWorkerProperties = DaemonWorkerProperties(
        pollingPeriod = Duration.ofMinutes(1),
        errorDelay = Duration.ofMinutes(1),
    ),
    val rateLimitMaxEntities: Int = 30,
    val rateLimitPeriod: Long = 10000,
)

data class UpdateSuspiciousItemsHandlerProperties(
    val enabled: Boolean = false,
    val chunkSize: Int = 2,
    val handlePeriod: Duration = Duration.ofDays(1),
    val awakePeriod: Duration = Duration.ofHours(1)
)

data class OwnershipCheckerProperties(
    val skipNumberOfBlocks: Long = 20,
    val confirms: Int = 2,
    val updateLastBlock: Duration = Duration.ofSeconds(5)
)

data class FixStandardJobProperties(
    val enabled: Boolean = true,
    val batchSize: Int = 500,
    val reduceBatch: Int = 2,
    val retries: Int = 2,
    val reindexLimit: Int = 2 // Max number of reindex tasks
)
