package com.rarible.protocol.order.listener.configuration

import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.protocol.order.core.model.OpenSeaFetchState
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.time.Duration
import java.time.Instant

internal const val RARIBLE_PROTOCOL_LISTENER = "listener"

@ConstructorBinding
@ConfigurationProperties(RARIBLE_PROTOCOL_LISTENER)
data class OrderListenerProperties(
    val monitoringWorker: DaemonWorkerProperties = DaemonWorkerProperties(),
    val publishTaskDelayMs: Long = 1000L,
    val parallelOrderUpdateStreams: Int = 10,
    val openSeaOrderSide: OrderSide? = null,
    val updateStatusByStartEndEnabled: Boolean = false,
    val updateAuctionOngoingStateEnabled: Boolean = false,
    val updateAuctionOngoingStateEndLag: Duration = Duration.ofMinutes(5),
    val openSeaClientUserAgents: String = "",
    val metricJobStartEnd: String = "",
    val ownershipConsumerWorkersCount: Int = 4,
    val zeroExExchangeDomainHash: String = "0x",
    val openSeaExchangeDomainHashV2: String = "0x0000000000000000000000000000000000000000000000000000000000000000",
    val openSeaOrdersLoadPeriodWorker: OpenSeaOrdersLoadPeriodWorkerProperties = OpenSeaOrdersLoadPeriodWorkerProperties(),
    val openSeaOrdersLoadWorker: OpenSeaOrderLoadWorkerProperties = OpenSeaOrderLoadWorkerProperties(),
    val openSeaOrdersLoadDelayWorker: OpenSeaOrderLoadWorkerProperties = OpenSeaOrderLoadWorkerProperties()
) {
    enum class OrderSide {
        ALL,
        SELL,
        BID
    }
}



sealed class BaseOpenSeaOrderLoadWorkerProperties {
    abstract val enabled: Boolean
    abstract val delay: Duration
    abstract val loadPeriod: Duration
    abstract val pollingPeriod: Duration
    abstract val errorDelay: Duration
    abstract val saveBatchSize: Int
    abstract val logPrefix: String
    abstract val workerName: String
    abstract val stateId: String
}

data class OpenSeaOrderLoadWorkerProperties(
    override val enabled: Boolean = false,
    override val delay: Duration = Duration.ofMinutes(3),
    override val loadPeriod: Duration = Duration.ofSeconds(1),
    override val pollingPeriod: Duration = Duration.ofSeconds(2),
    override val errorDelay: Duration = Duration.ofSeconds(5),
    override val saveBatchSize: Int = 100,
    override val logPrefix: String = "OpenSea",
    override val workerName: String = "open-sea-orders-load-worker",
    override val stateId: String = OpenSeaFetchState.ID
) : BaseOpenSeaOrderLoadWorkerProperties()

data class OpenSeaOrdersLoadPeriodWorkerProperties(
    override val enabled: Boolean = false,
    val start: Instant = Instant.now(),
    val end: Instant = Instant.now(),
    override val delay: Duration = Duration.ZERO,
    override val loadPeriod: Duration = Duration.ofSeconds(15),
    override val pollingPeriod: Duration = Duration.ofSeconds(2),
    override val errorDelay: Duration = Duration.ofSeconds(5),
    override val saveBatchSize: Int = 100,
    override val logPrefix: String = "OpenSeaPeriod",
    override val workerName: String = "",
    override val stateId: String = ""
) : BaseOpenSeaOrderLoadWorkerProperties()