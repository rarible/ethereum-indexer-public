package com.rarible.protocol.order.listener.service.descriptors.exchange.x2y2

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.contracts.x2y2.v1.events.EvInventoryEvent
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.service.ContractsProvider
import com.rarible.protocol.order.core.metric.ForeignOrderMetrics
import com.rarible.protocol.order.listener.service.descriptors.ExchangeSubscriber
import com.rarible.protocol.order.listener.service.x2y2.X2Y2EventConverter
import org.springframework.stereotype.Service
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

@Service
@CaptureSpan(type = SpanType.EVENT)
class X2Y2SellOrderMatchDescriptor(
    contractsProvider: ContractsProvider,
    private val converter: X2Y2EventConverter,
    private val metrics: ForeignOrderMetrics,
) : ExchangeSubscriber<OrderSideMatch>(
    name = "x2y2_inventory",
    topic = EvInventoryEvent.id(),
    contracts = contractsProvider.x2y2V1()
) {
    override suspend fun convert(log: Log, transaction: Transaction, timestamp: Instant, index: Int, totalLogs: Int): List<OrderSideMatch> {
        val event = EvInventoryEvent.apply(log)
        val converted = converter.convert(event, timestamp, transaction)
        metrics.onOrderEventHandled(Platform.X2Y2, "match")
        return converted
    }
}
