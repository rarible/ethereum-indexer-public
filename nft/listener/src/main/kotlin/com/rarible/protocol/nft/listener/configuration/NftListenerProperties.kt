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
)

data class ActionExecuteProperties(
    val enabled: Boolean = false,
    val daemon: DaemonWorkerProperties = DaemonWorkerProperties()
)

data class ItemOwnershipConsistencyProperties(
    val autofix: Boolean = true,
    val errorDelay: Duration = Duration.ofMinutes(1),
    val pollingPeriod: Duration = Duration.ofMinutes(1),
)