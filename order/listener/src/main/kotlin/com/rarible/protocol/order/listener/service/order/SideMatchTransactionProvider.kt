package com.rarible.protocol.order.listener.service.order

import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.RaribleMatchedOrders
import com.rarible.protocol.order.core.trace.TransactionTraceProvider
import io.daonomic.rpc.domain.Word
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class SideMatchTransactionProvider(
    private val traceProvider: TransactionTraceProvider,
    private val raribleOrderParser: RaribleExchangeV2OrderParser,
    properties: OrderIndexerProperties
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val raribleContract = properties.exchangeContractAddresses.v2

    suspend fun getMatchedOrdersByTransactionHash(transactionHash: Word): RaribleMatchedOrders? {
        return getTransactionTrace(transactionHash) { raribleOrderParser.parseMatchedOrders(it) }
    }

    private suspend fun <T> getTransactionTrace(transactionHash: Word, converter: (input: String) -> T): T? {
        val simpleTrace = traceProvider.getTransactionTrace(transactionHash)

        if (simpleTrace == null || simpleTrace.to != raribleContract) {
            logger.info("Empty trace (simpleTrace=$simpleTrace) or not Rarible contract ${simpleTrace?.to}")
            return null
        }
        return converter(simpleTrace.input)
    }
}
