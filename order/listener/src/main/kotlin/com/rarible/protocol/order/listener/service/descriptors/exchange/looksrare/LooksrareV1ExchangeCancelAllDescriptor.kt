package com.rarible.protocol.order.listener.service.descriptors.exchange.looksrare

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.exchange.looksrare.v1.CancelAllOrdersEvent
import com.rarible.protocol.order.core.model.ChangeNonceHistory
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.listener.service.descriptors.ContractsProvider
import com.rarible.protocol.order.listener.service.descriptors.NonceSubscriber
import org.springframework.stereotype.Service
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

@Service
@CaptureSpan(type = SpanType.EVENT)
class LooksrareV1ExchangeCancelAllDescriptor(
    contractsProvider: ContractsProvider,
    private val looksrareCancelAllEventMetric: RegisteredCounter
) : NonceSubscriber(
    topic = CancelAllOrdersEvent.id(),
    contracts = contractsProvider.looksrareV1()
) {
    override suspend fun convert(log: Log, transaction: Transaction, timestamp: Instant, index: Int, totalLogs: Int): List<ChangeNonceHistory> {
        val event = CancelAllOrdersEvent.apply(log)
        return listOf(
            ChangeNonceHistory(
                maker = event.user(),
                newNonce = EthUInt256.of(event.newMinNonce()),
                date = timestamp,
                source = HistorySource.LOOKSRARE
            )
        ).also { looksrareCancelAllEventMetric.increment() }
    }
}