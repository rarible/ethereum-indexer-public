package com.rarible.protocol.order.listener.service.opensea

import com.rarible.ethereum.domain.Blockchain
import com.rarible.opensea.client.model.OpenSeaOrder
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

@Primary
@Component
class MeasurableOpenSeaOrderService(
    private val delegate: OpenSeaOrderService,
    micrometer: MeterRegistry,
    blockchain: Blockchain
) : OpenSeaOrderService {
    private val counter = Counter.builder("protocol.opensea.order.load")
        .tag("blockchain", blockchain.value)
        .register(micrometer)

    override suspend fun getNextOrdersBatch(listedAfter: Long, listedBefore: Long): List<OpenSeaOrder> {
        return delegate.getNextOrdersBatch(listedAfter, listedBefore).also {
            counter.increment(it.size.toDouble())
        }
    }
}
