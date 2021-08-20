package com.rarible.protocol.order.listener.configuration

import com.rarible.core.daemon.DaemonWorkerProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.time.Duration

internal const val RARIBLE_PROTOCOL_LISTENER = "listener"

@ConstructorBinding
@ConfigurationProperties(RARIBLE_PROTOCOL_LISTENER)
class OrderListenerProperties(
    val monitoringWorker: DaemonWorkerProperties = DaemonWorkerProperties(),
    val priceUpdateEnabled: Boolean = false,
    val publishTaskDelayMs: Long = 1000L,
    val loadOpenSeaOrders: Boolean = true,
    val loadOpenSeaOrderVersion: Boolean = false,
    val loadOpenSeaPeriod: Duration = Duration.ofSeconds(1)
)
