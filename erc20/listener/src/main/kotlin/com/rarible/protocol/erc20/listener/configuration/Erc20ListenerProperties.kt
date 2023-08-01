package com.rarible.protocol.erc20.listener.configuration

import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.ethereum.domain.Blockchain
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.time.Duration

internal const val RARIBLE_PROTOCOL_LISTENER_STORAGE = "listener"

@ConstructorBinding
@ConfigurationProperties(RARIBLE_PROTOCOL_LISTENER_STORAGE)
data class Erc20ListenerProperties(
    val blockchain: Blockchain,
    val tokens: List<String> = emptyList(),
    val ignoredOwners: List<String> = emptyList(),
    val depositTokens: List<String> = emptyList(), // for these tokens we count deposit/withdrawal events
    val blockCountBeforeSnapshot: Int = 12,
    val logConsumeWorkerCount: Int = 9,
    val logConsumeWorkerBatchSize: Int = 500,
    val eventConsumerWorker: DaemonWorkerProperties = DaemonWorkerProperties(),
    val skipTransferContracts: List<String> = emptyList(),
    val balanceCheckerProperties: BalanceCheckerProperties = BalanceCheckerProperties(),
    val orderActivityProperties: OrderActivityProperties = OrderActivityProperties(),
    val job: Erc20JobProperties = Erc20JobProperties()
)

data class OrderActivityProperties(
    val eventsHandleBatchSize: Int = 200,
    val eventsHandleConcurrency: Int = 9,
)

data class BalanceCheckerProperties(
    val skipNumberOfBlocks: Long = 20,
    val confirms: Int = 2,
    val updateLastBlock: Duration = Duration.ofSeconds(5),
    val checkUpdatedAt: Boolean = false,
    val eventsHandleBatchSize: Int = 500,
    val eventsHandleConcurrency: Int = 1, // Fine for this worker, we don't need realtime here
)

data class Erc20JobProperties(
    val balanceCleanup: Erc20BalanceCleanupJobProperties = Erc20BalanceCleanupJobProperties()
)

data class Erc20BalanceCleanupJobProperties(
    val enabled: Boolean = false,
    val pollingPeriod: Duration = Duration.ofMinutes(1),
    val errorDelay: Duration = Duration.ofMinutes(1)
)
