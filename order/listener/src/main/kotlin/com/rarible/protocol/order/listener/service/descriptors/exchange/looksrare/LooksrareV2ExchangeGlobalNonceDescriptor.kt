package com.rarible.protocol.order.listener.service.descriptors.exchange.looksrare

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.exchange.looksrare.v2.NewBidAskNoncesEvent
import com.rarible.protocol.order.core.model.ChangeNonceHistory
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.service.ContractsProvider
import com.rarible.protocol.order.listener.service.descriptors.NonceSubscriber
import org.springframework.stereotype.Service
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

@Service
@CaptureSpan(type = SpanType.EVENT)
class LooksrareV2ExchangeGlobalNonceDescriptor(
    contractsProvider: ContractsProvider,
    private val looksrareCancelAllEventMetric: RegisteredCounter
) : NonceSubscriber(
    name = "lr_v2_new_bid_ask_nonces",
    topic = NewBidAskNoncesEvent.id(),
    contracts = contractsProvider.looksrareV2()
) {
    override suspend fun convert(log: Log, transaction: Transaction, timestamp: Instant, index: Int, totalLogs: Int): List<ChangeNonceHistory> {
        val event = NewBidAskNoncesEvent.apply(log)
        return listOf(
            ChangeNonceHistory(
                maker = event.user(),
                newNonce = EthUInt256.of(event.askNonce()),
                date = timestamp,
                source = HistorySource.LOOKSRARE
            )
        ).also { looksrareCancelAllEventMetric.increment() }
    }
}