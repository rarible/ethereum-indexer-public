package com.rarible.protocol.order.listener.service.descriptors.exchange.looksrare

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.contracts.exchange.looksrare.v2.SubsetNoncesCancelledEvent
import com.rarible.protocol.order.core.model.OrderCancel
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.ContractsProvider
import com.rarible.protocol.order.listener.misc.ForeignOrderMetrics
import org.springframework.stereotype.Service
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

@Service
@CaptureSpan(type = SpanType.EVENT)
class LooksrareV2ExchangeSubsetNoncesCanceledDescriptor(
    contractsProvider: ContractsProvider,
    orderRepository: OrderRepository,
    metrics: ForeignOrderMetrics
) : AbstractLooksrareExchangeDescriptor<OrderCancel>(
    name = "lr_v2_subset_nonces_cancelled",
    SubsetNoncesCancelledEvent.id(),
    contractsProvider.looksrareV2(),
    orderRepository,
    metrics
) {
    override suspend fun convert(log: Log, transaction: Transaction, timestamp: Instant, index: Int, totalLogs: Int): List<OrderCancel> {
        val event = SubsetNoncesCancelledEvent.apply(log)
        val nonces = event.subsetNonces().toList()
        val maker = event.user()
        return cancelBySubsetNonce(timestamp, maker, nonces)
    }
}