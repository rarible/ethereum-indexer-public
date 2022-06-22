package com.rarible.protocol.order.listener.service.opensea

import com.rarible.core.common.nowMillis
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.ethereum.domain.Blockchain
import com.rarible.opensea.client.model.v1.OpenSeaOrder
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component
import java.time.Duration
import javax.annotation.PostConstruct

@Component
class MeasurableOpenSeaOrderWrapper(
    private val micrometer: MeterRegistry,
    private val blockchain: Blockchain
) {
    fun wrap(
        delegate: OpenSeaOrderService,
        loadCounter: RegisteredCounter,
        measureDelay: Boolean
    ): OpenSeaOrderService {
        return MeasurableOpenSeaOrderService(
            delegate = delegate,
            micrometer = micrometer,
            blockchain = blockchain,
            loadCounter = loadCounter,
            measureDelay = measureDelay
        )
    }
}

class MeasurableOpenSeaOrderService(
    private val delegate: OpenSeaOrderService,
    private val micrometer: MeterRegistry,
    private val blockchain: Blockchain,
    private val loadCounter: RegisteredCounter,
    private val measureDelay: Boolean
) : OpenSeaOrderService {
    @Volatile private var latestSeanOpenSeaOrderTimestamp: Long? = null

    init {
        initMetrics()
    }

    override suspend fun getNextOrdersBatch(
        listedAfter: Long,
        listedBefore: Long,
        loadPeriod: Duration,
        logPrefix: String
    ): List<OpenSeaOrder> {
        return delegate.getNextOrdersBatch(listedAfter, listedBefore, loadPeriod, logPrefix).also { orders ->
            orders
                .maxOfOrNull { it.createdAt.epochSecond }
                ?.let {
                    latestSeanOpenSeaOrderTimestamp = it
                }

            loadCounter.increment(orders.size.toDouble())
        }
    }

    private fun getLoadOpenSeaDelay(): Double {
        val now = nowMillis().epochSecond
        val last = latestSeanOpenSeaOrderTimestamp ?: now
        return (now - last).toDouble()
    }

    @PostConstruct
    fun initMetrics() {
        if (measureDelay) {
            Gauge.builder("protocol.opensea.order.delay", this::getLoadOpenSeaDelay)
                .tag("blockchain", blockchain.value)
                .register(micrometer)
        }
    }
}
