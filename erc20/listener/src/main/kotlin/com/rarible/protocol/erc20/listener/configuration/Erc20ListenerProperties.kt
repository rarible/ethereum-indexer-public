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
    val blockCountBeforeSnapshot: Int = 12,
    val logConsumeWorkerCount: Int = 10,
    val eventConsumerWorker: DaemonWorkerProperties = DaemonWorkerProperties(),
    val skipTransferContracts: List<String> = emptyList(),
    val balanceCheckerProperties: BalanceCheckerProperties = BalanceCheckerProperties()
)

data class BalanceCheckerProperties(
    val skipNumberOfBlocks: Long = 20,
    val delay: Duration = Duration.ofSeconds(25),
    val updateLastBlock: Duration = Duration.ofSeconds(5)
)
