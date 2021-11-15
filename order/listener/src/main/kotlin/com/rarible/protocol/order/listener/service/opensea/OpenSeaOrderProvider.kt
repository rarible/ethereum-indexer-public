package com.rarible.protocol.order.listener.service.opensea

import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.OpenSeaMatchedOrders
import com.rarible.protocol.order.core.model.OpenSeaTransactionOrder
import com.rarible.protocol.order.core.trace.TransactionTraceProvider
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class OpenSeaOrderProvider(
    private val traceProvider: TransactionTraceProvider,
    private val openSeaOrderParser: OpenSeaOrderParser,
    properties: OrderIndexerProperties
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val openSeaContract = properties.exchangeContractAddresses.openSeaV1

    suspend fun getMatchedOrdersByTransactionHash(transactionHash: Word): OpenSeaMatchedOrders? {
        return getTransactionTrace(transactionHash) { openSeaOrderParser.safeParseMatchedOrders(it) }
    }

    suspend fun getCancelOrderByTransactionHash(transactionHash: Word): OpenSeaTransactionOrder? {
        return getTransactionTrace(transactionHash) { openSeaOrderParser.safeParseCancelOrder(it) }
    }

    private suspend fun <T> getTransactionTrace(transactionHash: Word, converter: (input: Binary) -> T): T? {
        val simpleTrace = traceProvider.getTransactionTrace(transactionHash)

        if (simpleTrace == null || simpleTrace.to != openSeaContract) {
            logger.info("Empty trace (simpleTrace=$simpleTrace) or not OpenSea contract ${simpleTrace?.to}")
            return null
        }
        return converter(Binary.apply(simpleTrace.input))
    }
}
