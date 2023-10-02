package com.rarible.protocol.order.listener.service.descriptors.exchange.v2

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.protocol.contracts.exchange.v2.events.CancelEventDeprecated
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.OrderCancel
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.ContractsProvider
import com.rarible.protocol.order.listener.service.descriptors.AutoReduceService
import com.rarible.protocol.order.listener.service.descriptors.ExchangeSubscriber
import io.daonomic.rpc.domain.Word
import org.springframework.stereotype.Service
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

@Service
@CaptureSpan(type = SpanType.EVENT)
class ExchangeV2CancelDeprecatedDescriptor(
    contractsProvider: ContractsProvider,
    private val orderRepository: OrderRepository,
    private val raribleCancelEventMetric: RegisteredCounter,
    autoReduceService: AutoReduceService,
) : ExchangeSubscriber<OrderCancel>(
    name = "rari_v2_cancel_deprecated",
    topic = CancelEventDeprecated.id(),
    contracts = contractsProvider.raribleExchangeV2(),
    autoReduceService = autoReduceService,
) {
    override suspend fun convert(log: Log, transaction: Transaction, timestamp: Instant, index: Int, totalLogs: Int): List<OrderCancel> {
        val event = CancelEventDeprecated.apply(log)
        val hash = Word.apply(event.hash())
        val order = orderRepository.findById(hash)
        return listOf(
            OrderCancel(
                hash = hash,
                make = order?.make,
                take = order?.take,
                date = timestamp,
                maker = order?.maker,
                source = HistorySource.RARIBLE
            )
        ).also { raribleCancelEventMetric.increment() }
    }
}
