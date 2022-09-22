package com.rarible.protocol.order.listener.service.opensea

import com.rarible.core.common.nowMillis
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.core.telemetry.metrics.RegisteredGauge
import com.rarible.ethereum.domain.Blockchain
import com.rarible.opensea.client.model.v2.SeaportOrders
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry

class MeasurableOpenSeaOrderService(
    private val delegate: OpenSeaOrderService,
    private val micrometer: MeterRegistry,
    private val blockchain: Blockchain,
    private val seaportLoadCounter: RegisteredCounter,
    private val seaportDelayGauge : RegisteredGauge<Long>,
    private val measureDelay: Boolean
) : OpenSeaOrderService {
    @Volatile private var latestSeanOpenSeaOrderTimestamp: Long? = null

    init {
        initMetrics()
    }

    override suspend fun getNextSellOrders(nextCursor: String?, loadAhead: Boolean): SeaportOrders {
        val orders = delegate.getNextSellOrders(nextCursor, loadAhead)
        seaportLoadCounter.increment(orders.orders.size)
        if (measureDelay) {
            orders.orders.maxOfOrNull { it.createdAt }?.let {
                seaportDelayGauge.set(nowMillis().epochSecond - it.epochSecond)
            }
        }
        return orders
    }

    private fun getLoadOpenSeaDelay(): Double {
        val now = nowMillis().epochSecond
        val last = latestSeanOpenSeaOrderTimestamp ?: now
        return (now - last).toDouble()
    }

    private fun initMetrics() {
        if (measureDelay) {
            Gauge.builder("protocol.opensea.order.delay", this::getLoadOpenSeaDelay)
                .tag("blockchain", blockchain.value)
                .register(micrometer)
        }
    }
}
