package com.rarible.protocol.order.listener.service.opensea

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.Blockchain
import com.rarible.opensea.client.model.OpenSeaOrder
import io.micrometer.core.instrument.Counter
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
    private val blockchain: Blockchain
) : OpenSeaOrderService {
    @Volatile private var latestSeanOpenSeaOrderTimestamp: Long? = null

    private val counter = Counter.builder("protocol.opensea.order.load")
        .tag("blockchain", blockchain.value)
        .register(micrometer)

    override suspend fun getNextOrdersBatch(listedAfter: Long, listedBefore: Long): List<OpenSeaOrder> {
        return delegate.getNextOrdersBatch(listedAfter, listedBefore).also { orders ->
            orders
                .maxOfOrNull { it.createdAt.epochSecond }
                ?.let {
                    latestSeanOpenSeaOrderTimestamp = it
                }

            counter.increment(orders.size.toDouble())
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
