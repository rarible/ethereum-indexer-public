package com.rarible.protocol.order.listener.service.descriptors.exchange.x2y2

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.protocol.contracts.exchange.x2y2.v1.EvCancelEvent
import com.rarible.protocol.order.core.model.OrderCancel
import com.rarible.protocol.order.core.service.ContractsProvider
import com.rarible.protocol.order.listener.service.descriptors.ExchangeSubscriber
import com.rarible.protocol.order.listener.service.x2y2.X2Y2EventConverter
import java.time.Instant
import org.springframework.stereotype.Service
import scalether.domain.response.Log
import scalether.domain.response.Transaction

@Service
@CaptureSpan(type = SpanType.EVENT)
class X2Y2OrderCancelDescriptor(
    contractsProvider: ContractsProvider,
    private val converter: X2Y2EventConverter,
    private val x2y2CancelEventCounter: RegisteredCounter
) : ExchangeSubscriber<OrderCancel>(
    topic = EvCancelEvent.id(),
    contracts = contractsProvider.x2y2V1()
) {
    override suspend fun convert(log: Log, transaction: Transaction, timestamp: Instant, index: Int, totalLogs: Int): List<OrderCancel> {
        val event = EvCancelEvent.apply(log)
        val converted = converter.convert(event, timestamp)
        x2y2CancelEventCounter.increment()
        return listOf(converted)
    }
}