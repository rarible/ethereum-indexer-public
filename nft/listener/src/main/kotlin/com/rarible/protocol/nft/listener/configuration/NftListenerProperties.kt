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
    val logConsumeWorkerCount: Int = 10,
    val monitoringWorker: DaemonWorkerProperties = DaemonWorkerProperties(),
    val eventConsumerWorker: DaemonWorkerProperties = DaemonWorkerProperties(),
    val enableCheckDataQualityJob: Boolean = false,
    var elementsFetchJobSize: Int = 1000,
    val actionExecute: ActionExecuteProperties = ActionExecuteProperties(),
    val itemOwnershipConsistency: ItemOwnershipConsistencyProperties = ItemOwnershipConsistencyProperties(),
    val ownershipItemConsistency: OwnershipItemConsistencyProperties = OwnershipItemConsistencyProperties(),
    val inconsistentItemsRepair: InconsistentItemsRepairProperties = InconsistentItemsRepairProperties(),
    val updateSuspiciousItemsHandler: UpdateSuspiciousItemsHandlerProperties = UpdateSuspiciousItemsHandlerProperties(),
)

data class ActionExecuteProperties(
    val enabled: Boolean = false,
    val daemon: DaemonWorkerProperties = DaemonWorkerProperties()
)

data class ItemOwnershipConsistencyProperties(
    val autofix: Boolean = true,
    val checkTimeOffset: Duration = Duration.ofSeconds(30),
    val parallelism: Int = 16,
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
    val parallelism: Int = 4,
    val daemon: DaemonWorkerProperties = DaemonWorkerProperties(
        pollingPeriod = Duration.ofMinutes(1),
        errorDelay = Duration.ofMinutes(1),
    ),
    val rateLimitMaxEntities: Int = 100,
    val rateLimitPeriod: Long = 10000,
)

data class UpdateSuspiciousItemsHandlerProperties(
    val enabled: Boolean = false,
    val chunkSize: Int = 100,
    val handlePeriod: Duration = Duration.ofDays(14),
    val awakePeriod: Duration = Duration.ofHours(1)
)
