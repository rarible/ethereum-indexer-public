package com.rarible.protocol.order.listener.service.descriptors.exchange.opensea

import com.rarible.protocol.contracts.exchange.seaport.v1.OrderCancelledEvent
import com.rarible.protocol.order.core.model.OrderCancel
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.service.ContractsProvider
import com.rarible.protocol.order.listener.misc.ForeignOrderMetrics
import com.rarible.protocol.order.listener.service.descriptors.AutoReduceService
import com.rarible.protocol.order.listener.service.descriptors.ExchangeSubscriber
import com.rarible.protocol.order.listener.service.opensea.SeaportEventConverter
import org.springframework.stereotype.Service
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

@Service
class SeaportV1ExchangeCancelDescriptor(
    contractsProvider: ContractsProvider,
    private val seaportEventConverter: SeaportEventConverter,
    private val metrics: ForeignOrderMetrics,
    autoReduceService: AutoReduceService,
) : ExchangeSubscriber<OrderCancel>(
    name = "os_cancelled",
    topic = OrderCancelledEvent.id(),
    contracts = contractsProvider.seaportV1(),
    autoReduceService = autoReduceService,
) {
    override suspend fun convert(log: Log, transaction: Transaction, timestamp: Instant, index: Int, totalLogs: Int): List<OrderCancel> {
        val event = OrderCancelledEvent.apply(log)
        return seaportEventConverter
            .convert(event, transaction, index, totalLogs, timestamp)
            .also { metrics.onOrderEventHandled(Platform.OPEN_SEA, "cancel") }
    }
}
