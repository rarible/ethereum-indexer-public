package com.rarible.protocol.order.listener.service.descriptors.exchange.looksrare

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.protocol.contracts.exchange.looksrare.v1.CancelMultipleOrdersEvent
import com.rarible.protocol.order.core.model.OrderCancel
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.ContractsProvider
import org.springframework.stereotype.Service
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

@Service
@CaptureSpan(type = SpanType.EVENT)
class LooksrareV1ExchangeCancelDescriptor(
    contractsProvider: ContractsProvider,
    orderRepository: OrderRepository,
    looksrareCancelOrdersEventMetric: RegisteredCounter,
) : AbstractLooksrareExchangeDescriptor<OrderCancel>(
    CancelMultipleOrdersEvent.id(),
    contractsProvider,
    orderRepository,
    looksrareCancelOrdersEventMetric
) {
    override suspend fun convert(log: Log, transaction: Transaction, timestamp: Instant, index: Int, totalLogs: Int): List<OrderCancel> {
        val event = CancelMultipleOrdersEvent.apply(log)
        val nonces = event.orderNonces().map { it.toLong() }
        val maker = event.user()
        return cancelUserOrders(timestamp, maker, nonces)
    }
}
