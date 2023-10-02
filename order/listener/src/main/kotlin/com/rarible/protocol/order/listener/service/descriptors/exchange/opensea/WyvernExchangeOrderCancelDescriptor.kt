package com.rarible.protocol.order.listener.service.descriptors.exchange.opensea

import com.rarible.protocol.contracts.exchange.wyvern.OrderCancelledEvent
import com.rarible.protocol.order.core.model.OrderCancel
import com.rarible.protocol.order.core.service.ContractsProvider
import com.rarible.protocol.order.listener.service.descriptors.AutoReduceService
import com.rarible.protocol.order.listener.service.descriptors.ExchangeSubscriber
import com.rarible.protocol.order.listener.service.opensea.OpenSeaOrderEventConverter
import com.rarible.protocol.order.listener.service.opensea.OpenSeaOrderParser
import org.slf4j.LoggerFactory
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

// @Service //TODO: Activate after move to a new scanner
class WyvernExchangeOrderCancelDescriptor(
    private val contractsProvider: ContractsProvider,
    private val openSeaOrderEventConverter: OpenSeaOrderEventConverter,
    private val openSeaOrderParser: OpenSeaOrderParser,
    autoReduceService: AutoReduceService,
) : ExchangeSubscriber<OrderCancel>(
    name = "os_cancelled",
    topic = OrderCancelledEvent.id(),
    contracts = contractsProvider.openSea(),
    autoReduceService = autoReduceService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun convert(log: Log, transaction: Transaction, timestamp: Instant, index: Int, totalLogs: Int): List<OrderCancel> {
        val transactionHash = log.transactionHash()
        val event = OrderCancelledEvent.apply(log)
        logger.info("Got OrderCancel event, tx=$transactionHash")

        val order = openSeaOrderParser.parseCancelOrder(transaction.input())
        return if (order != null) {
            openSeaOrderEventConverter.convert(order, timestamp, event, log.address() == contractsProvider.openSeaV2())
        } else {
            logger.warn("Can't parser OpenSea cancel transaction ${transaction.value()}")
            emptyList()
        }
    }
}
