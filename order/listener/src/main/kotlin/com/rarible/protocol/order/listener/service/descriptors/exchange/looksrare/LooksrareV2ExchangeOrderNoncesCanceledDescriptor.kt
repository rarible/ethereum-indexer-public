package com.rarible.protocol.order.listener.service.descriptors.exchange.looksrare

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.protocol.contracts.exchange.looksrare.v2.OrderNoncesCancelledEvent
import com.rarible.protocol.order.core.model.OrderCancel
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.ContractsProvider
import org.springframework.stereotype.Service
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

@Service
@CaptureSpan(type = SpanType.EVENT)
class LooksrareV2ExchangeOrderNoncesCanceledDescriptor(
    contractsProvider: ContractsProvider,
    orderRepository: OrderRepository,
    looksrareCancelOrdersEventMetric: RegisteredCounter,
) : AbstractLooksrareExchangeDescriptor<OrderCancel>(
    name = "lr_v2_nonces_cancelled",
    OrderNoncesCancelledEvent.id(),
    contractsProvider.looksrareV2(),
    orderRepository,
    looksrareCancelOrdersEventMetric
) {
    override suspend fun convert(log: Log, transaction: Transaction, timestamp: Instant, index: Int, totalLogs: Int): List<OrderCancel> {
        val event = OrderNoncesCancelledEvent.apply(log)
        val nonces = event.orderNonces().toList()
        val maker = event.user()
        return cancelByOrderNonce(timestamp, maker, nonces)
    }
}
