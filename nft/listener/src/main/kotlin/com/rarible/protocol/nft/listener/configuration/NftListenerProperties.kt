package com.rarible.protocol.nft.listener.configuration

import com.rarible.core.daemon.DaemonWorkerProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

internal const val RARIBLE_PROTOCOL_LISTENER_STORAGE = "listener"

@ConstructorBinding
@ConfigurationProperties(RARIBLE_PROTOCOL_LISTENER_STORAGE)
data class NftListenerProperties(
    val skipReduceTokens: List<String> = emptyList(),
    val skipContracts: List<String> = emptyList(),
    val skipTransferContracts: List<String> = emptyList(),
    val logConsumeWorkerCount: Int = 10,
    val monitoringWorker: DaemonWorkerProperties = DaemonWorkerProperties(),
    val eventConsumerWorker: DaemonWorkerProperties = DaemonWorkerProperties(),
    val enableCheckDataQualityJob: Boolean = false,
    var elementsFetchJobSize: Int = 1000
)
