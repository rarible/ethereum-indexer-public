package com.rarible.protocol.order.listener.service.descriptors.exchange.opensea

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.exchange.seaport.v1.CounterIncrementedEvent
import com.rarible.protocol.order.core.model.ChangeNonceHistory
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.service.ContractsProvider
import com.rarible.protocol.order.listener.misc.ForeignOrderMetrics
import com.rarible.protocol.order.listener.service.descriptors.AutoReduceService
import com.rarible.protocol.order.listener.service.descriptors.NonceSubscriber
import org.springframework.stereotype.Service
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

@Service
class SeaportExchangeChangeCounterDescriptor(
    contractsProvider: ContractsProvider,
    private val metrics: ForeignOrderMetrics,
    autoReduceService: AutoReduceService,
) : NonceSubscriber(
    name = "os_counter_incremented",
    topic = CounterIncrementedEvent.id(),
    contracts = contractsProvider.seaportV1(),
    autoReduceService = autoReduceService,
) {
    override suspend fun convert(log: Log, transaction: Transaction, timestamp: Instant, index: Int, totalLogs: Int): List<ChangeNonceHistory> {
        val event = CounterIncrementedEvent.apply(log)
        return listOf(
            ChangeNonceHistory(
                maker = event.offerer(),
                newNonce = EthUInt256.of(event.newCounter()),
                date = timestamp,
                source = HistorySource.OPEN_SEA
            )
        ).also { metrics.onOrderEventHandled(Platform.OPEN_SEA, "counter") }
    }
}
