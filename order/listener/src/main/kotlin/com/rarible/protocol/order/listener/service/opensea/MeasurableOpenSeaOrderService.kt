package com.rarible.protocol.order.listener.service.opensea

import com.rarible.core.common.nowMillis
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.ethereum.domain.Blockchain
import com.rarible.opensea.client.model.OpenSeaOrder
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Primary
@Component
class MeasurableOpenSeaOrderService(
    private val delegate: OpenSeaOrderService,
    private val micrometer: MeterRegistry,
    private val blockchain: Blockchain,
    private val openSeaOrderLoadRegisteredCounter: RegisteredCounter
) : OpenSeaOrderService {
    @Volatile private var latestSeanOpenSeaOrderTimestamp: Long? = null

    override suspend fun getNextOrdersBatch(listedAfter: Long, listedBefore: Long, logPrefix: String): List<OpenSeaOrder> {
        return delegate.getNextOrdersBatch(listedAfter, listedBefore, logPrefix).also { orders ->
            orders
                .maxOfOrNull { it.createdAt.epochSecond }
                ?.let {
                    latestSeanOpenSeaOrderTimestamp = it
                }

            openSeaOrderLoadRegisteredCounter.increment(orders.size.toDouble())
        }
    }

    private fun getLoadOpenSeaDelay(): Double {
        val now = nowMillis().epochSecond
        val last = latestSeanOpenSeaOrderTimestamp ?: now
        return (now - last).toDouble()
    }

    @PostConstruct
    fun initMetrics() {
        Gauge.builder("protocol.opensea.order.delay", this::getLoadOpenSeaDelay)
            .tag("blockchain", blockchain.value)
            .register(micrometer)
    }
}
