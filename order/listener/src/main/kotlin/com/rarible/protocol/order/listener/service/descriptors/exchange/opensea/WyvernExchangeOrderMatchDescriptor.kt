package com.rarible.protocol.order.listener.service.descriptors.exchange.opensea

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.contracts.exchange.wyvern.OrdersMatchedEvent
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.core.service.ContractsProvider
import com.rarible.protocol.order.listener.service.descriptors.ExchangeSubscriber
import com.rarible.protocol.order.listener.service.opensea.OpenSeaOrderEventConverter
import com.rarible.protocol.order.listener.service.opensea.OpenSeaOrderParser
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

// @Service //TODO: Activate after move to a new scanner
@CaptureSpan(type = SpanType.EVENT)
class WyvernExchangeOrderMatchDescriptor(
    private val contractsProvider: ContractsProvider,
    private val openSeaOrdersSideMatcher: OpenSeaOrderEventConverter,
    private val openSeaOrderParser: OpenSeaOrderParser
) : ExchangeSubscriber<OrderSideMatch>(
    name = "os_matched",
    topic = OrdersMatchedEvent.id(),
    contracts = contractsProvider.openSea()
) {
    override suspend fun convert(log: Log, transaction: Transaction, timestamp: Instant, index: Int, totalLogs: Int): List<OrderSideMatch> {
        val event = OrdersMatchedEvent.apply(log)
        val eip712 = log.address() == contractsProvider.openSeaV2()

        val orders = openSeaOrderParser.parseMatchedOrders(
            txHash = transaction.hash(),
            txInput = transaction.input(),
            event = event,
            index = index,
            totalLogs = totalLogs,
            eip712 = eip712
        )
        return if (orders != null) {
            openSeaOrdersSideMatcher.convert(
                openSeaOrders = orders,
                from = transaction.from(),
                price = event.price(),
                date = timestamp,
                input = transaction.input()
            )
        } else {
            emptyList()
        }
    }
}
