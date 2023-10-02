package com.rarible.protocol.order.listener.service.descriptors.exchange.x2y2

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.contracts.exchange.x2y2.v1.EvCancelEvent
import com.rarible.protocol.order.core.model.OrderCancel
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.service.ContractsProvider
import com.rarible.protocol.order.listener.misc.ForeignOrderMetrics
import com.rarible.protocol.order.listener.service.descriptors.AutoReduceService
import com.rarible.protocol.order.listener.service.descriptors.ExchangeSubscriber
import com.rarible.protocol.order.listener.service.x2y2.X2Y2EventConverter
import org.springframework.stereotype.Service
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

@Service
@CaptureSpan(type = SpanType.EVENT)
class X2Y2OrderCancelDescriptor(
    contractsProvider: ContractsProvider,
    private val converter: X2Y2EventConverter,
    private val metrics: ForeignOrderMetrics,
    autoReduceService: AutoReduceService,
) : ExchangeSubscriber<OrderCancel>(
    name = "x2y2_cancel",
    topic = EvCancelEvent.id(),
    contracts = contractsProvider.x2y2V1(),
    autoReduceService = autoReduceService,
) {
    override suspend fun convert(log: Log, transaction: Transaction, timestamp: Instant, index: Int, totalLogs: Int): List<OrderCancel> {
        val event = EvCancelEvent.apply(log)
        val converted = converter.convert(event, timestamp)
        metrics.onOrderEventHandled(Platform.X2Y2, "cancel")
        return listOf(converted)
    }
}
