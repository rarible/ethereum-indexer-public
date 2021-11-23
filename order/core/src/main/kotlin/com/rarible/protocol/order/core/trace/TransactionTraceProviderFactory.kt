package com.rarible.protocol.order.core.trace

import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.NodeType
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import scalether.core.MonoEthereum

@Component
class TransactionTraceProviderFactory(
    private val ethereum: MonoEthereum,
    properties: OrderIndexerProperties
) {
    private val transactionTraceProvider: TransactionTraceProvider = runBlocking {
        if (properties.nodeType == null) {
            logger.warn("nodeType not set. using OPEN_ETHEREUM")
        }
        when (properties.nodeType ?: NodeType.OPEN_ETHEREUM) {
            NodeType.OPEN_ETHEREUM -> {
                OpenEthereumTransactionTraceProvider(ethereum)
            }
            NodeType.GETH, NodeType.UNKNOWN -> {
                GethTransactionTraceProvider(ethereum)
            }
        }
    }

    fun createTraceProvider(): TransactionTraceProvider {
        return transactionTraceProvider
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(TransactionTraceProviderFactory::class.java)
    }
}
