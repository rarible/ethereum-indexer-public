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
    val openSeaOrdersLoadDelayWorker: OpenSeaOrderLoadWorkerProperties = OpenSeaOrderLoadWorkerProperties(),
    val raribleExpiredBidWorker: RaribleExpiredBidWorkerProperties = RaribleExpiredBidWorkerProperties(),
    val seaportLoad: SeaportLoadProperties = SeaportLoadProperties(),
    val looksrareLoad: LooksrareLoadProperties = LooksrareLoadProperties(),
    val x2y2Load: X2Y2LoadProperties = X2Y2LoadProperties(),
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
    val retryDelay: Duration = Duration.ofMillis(500),
    val pollingPeriod: Duration = Duration.ofSeconds(10),
    val errorDelay: Duration = Duration.ofSeconds(5)
)

data class LooksrareLoadProperties(
    val enabled: Boolean = false,
    var saveEnabled: Boolean = false,
    val delay: Duration = Duration.ofMinutes(1),
    val loadPeriod: Duration = Duration.ofSeconds(5),
    val loadMaxSize: Int = 150,
    val retry: Int = 5,
    val saveBatchSize: Int = 50,
    val retryDelay: Duration = Duration.ofMillis(500),
    val pollingPeriod: Duration = Duration.ofSeconds(5),
    val errorDelay: Duration = Duration.ofSeconds(5)
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

data class X2Y2LoadProperties(
    val enabled: Boolean = false,
    val saveEnabled: Boolean = false,
    val saveBatchSize: Int = 50,
    val startCursor: Long? = 1657843200000, // Friday, July 15, 2022 12:00:00 AM
    val retryDelay: Duration = Duration.ofMillis(500),
    val pollingPeriod: Duration = Duration.ofSeconds(10),
    val errorDelay: Duration = Duration.ofSeconds(5),
)
