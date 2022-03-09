package com.rarible.protocol.order.listener.configuration

import com.rarible.core.daemon.DaemonWorkerProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.time.Duration

internal const val RARIBLE_PROTOCOL_LISTENER = "listener"

@ConstructorBinding
@ConfigurationProperties(RARIBLE_PROTOCOL_LISTENER)
data class OrderListenerProperties(
    val monitoringWorker: DaemonWorkerProperties = DaemonWorkerProperties(),
    val publishTaskDelayMs: Long = 1000L,
    val parallelOrderUpdateStreams: Int = 10,
    val loadOpenSeaOrders: Boolean = false,
    val loadOpenSeaPeriod: Duration = Duration.ofSeconds(1),
    val loadOpenSeaDelay: Duration = Duration.ofSeconds(5),
    val saveOpenSeaOrdersBatchSize: Int = 100,
    val openSeaOrderSide: OrderSide? = null,
    val updateStatusByStartEndEnabled: Boolean = false,
    val updateAuctionOngoingStateEnabled: Boolean = false,
    val updateAuctionOngoingStateEndLag: Duration = Duration.ofMinutes(5),
    val openSeaClientUserAgents: String = "",
    val metricJobStartEnd: String = ""
) {
    enum class OrderSide {
        ALL,
        SELL,
        BID
    }
}
