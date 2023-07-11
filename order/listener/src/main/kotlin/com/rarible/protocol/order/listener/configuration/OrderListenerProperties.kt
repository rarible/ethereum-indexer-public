package com.rarible.protocol.order.listener.configuration

import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.protocol.order.core.model.OpenSeaFetchState
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import scalether.domain.Address
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
    val updateAuctionOngoingStateEnabled: Boolean = false,
    val updateAuctionOngoingStateEndLag: Duration = Duration.ofMinutes(5),
    val openSeaClientUserAgents: String = "",
    val balanceConsumerWorkersCount: Int = 9,
    val balanceConsumerBatchSize: Int = 500,
    val ownershipConsumerWorkersCount: Int = 9,
    val ownershipConsumerBatchSize: Int = 500,
    val itemConsumerWorkersCount: Int = 9,
    val itemConsumerBatchSize: Int = 500,
    val zeroExExchangeDomainHash: String = "0x",
    val openSeaExchangeDomainHashV2: String = "0x0000000000000000000000000000000000000000000000000000000000000000",
    val openSeaOrdersLoadPeriodWorker: OpenSeaOrdersLoadPeriodWorkerProperties = OpenSeaOrdersLoadPeriodWorkerProperties(),
    val openSeaOrdersLoadWorker: OpenSeaOrderLoadWorkerProperties = OpenSeaOrderLoadWorkerProperties(),
    val openSeaOrdersLoadDelayWorker: OpenSeaOrderLoadWorkerProperties = OpenSeaOrderLoadWorkerProperties(),
    val raribleExpiredBidWorker: RaribleExpiredBidWorkerProperties = RaribleExpiredBidWorkerProperties(),
    val seaportLoad: SeaportLoadProperties = SeaportLoadProperties(),
    val reservoir: ReservoirProperties = ReservoirProperties(),
    val x2y2Load: X2Y2OrderLoadProperties = X2Y2OrderLoadProperties(),
    val x2y2CancelListEventLoad: X2Y2EventLoadProperties = X2Y2EventLoadProperties(),
    val sudoSwapLoad: SudoSwapLoadProperties = SudoSwapLoadProperties(),
    val startEndWorker: StartEndWorkerProperties = StartEndWorkerProperties(),
    val floorOrderCheckWorker: FloorOrderCheckWorkerProperties = FloorOrderCheckWorkerProperties(),
    var fixApproval: Boolean = false,
    var fixX2Y2: Boolean = false,
    val approvalEvenHandleDelay: Duration = Duration.ZERO,
    val eventConsumerWorker: DaemonWorkerProperties = DaemonWorkerProperties(),
    val logConsumeWorkerCount: Int = 9,
    val logConsumeWorkerBatchSize: Int = 500,
    val floorPriceTopCollectionsCount: Int = 10
) {
    enum class OrderSide {
        ALL,
        SELL,
        BID
    }
}

data class SeaportLoadProperties(
    val enabled: Boolean = false,
    val saveEnabled: Boolean = false,
    val retry: Int = 5,
    val saveBatchSize: Int = 50,
    val loadMaxSize: Int = 50,
    val maxLoadResults: Int = 10,
    var asyncRequestsEnabled: Boolean = false,
    var maxAsyncRequests: Int = 5,
    val retryDelay: Duration = Duration.ofMillis(500),
    val pollingPeriod: Duration = Duration.ofSeconds(10),
    val errorDelay: Duration = Duration.ofSeconds(5),
    val validateSignature: Boolean = true,
    val ignoredSellTokens: List<Address> = emptyList()
)

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

data class RaribleExpiredBidWorkerProperties(
    val enabled: Boolean = true,
    val pollingPeriod: Duration = Duration.ofMinutes(10)
)

data class StartEndWorkerProperties(
    val enabled: Boolean = true,
    val cancelOffset: Duration = Duration.ofMinutes(1),
    val pollingPeriod: Duration = Duration.ofMinutes(1),
    val errorDelay: Duration = Duration.ofMinutes(2)
)

data class FloorOrderCheckWorkerProperties(
    val enabled: Boolean = true,
    val pollingPeriod: Duration = Duration.ofMinutes(60),
    val errorDelay: Duration = Duration.ofMinutes(60)
)

sealed class X2Y2LoadProperties {
    abstract val enabled: Boolean
    abstract val startCursor: Long?
    abstract val retryDelay: Duration
    abstract val pollingPeriod: Duration
    abstract val errorDelay: Duration
}

data class X2Y2OrderLoadProperties(
    override val enabled: Boolean = false,
    val saveEnabled: Boolean = false,
    val saveBatchSize: Int = 50,
    override val startCursor: Long? = 1657843200000, // Friday, July 15, 2022 12:00:00 AM
    override val retryDelay: Duration = Duration.ofMillis(500),
    override val pollingPeriod: Duration = Duration.ofSeconds(10),
    override val errorDelay: Duration = Duration.ofSeconds(5),
) : X2Y2LoadProperties()

data class X2Y2EventLoadProperties(
    override val enabled: Boolean = false,
    override val startCursor: Long? = 1657843200, // Friday, July 15, 2022 12:00:00 AM
    override val retryDelay: Duration = Duration.ofMillis(500),
    override val pollingPeriod: Duration = Duration.ofSeconds(10),
    override val errorDelay: Duration = Duration.ofSeconds(5),
) : X2Y2LoadProperties()

data class SudoSwapLoadProperties(
    val ignorePairs: Set<Address> = emptySet(),
)

data class ReservoirProperties(
    val enabled: Boolean = false,
    val cancelEnabled: Boolean = false,
    val size: Long = 50,
    val pollingPeriod: Duration = Duration.ofSeconds(60),
    val errorDelay: Duration = Duration.ofSeconds(1)
)