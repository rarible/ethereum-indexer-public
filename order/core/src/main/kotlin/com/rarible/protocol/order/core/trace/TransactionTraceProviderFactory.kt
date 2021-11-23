package com.rarible.protocol.order.core.trace

import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.NodeType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import scalether.core.MonoEthereum

@Configuration
class TransactionTraceProviderFactory(
    private val ethereum: MonoEthereum,
    private val properties: OrderIndexerProperties
) {

    @Bean
    fun traceProvider(): TransactionTraceProvider {
        if (properties.nodeType == null) {
            logger.warn("nodeType not set. using OPEN_ETHEREUM")
        }
        return when (properties.nodeType ?: NodeType.OPEN_ETHEREUM) {
            NodeType.OPEN_ETHEREUM -> {
                OpenEthereumTransactionTraceProvider(ethereum)
            }
            NodeType.GETH, NodeType.UNKNOWN -> {
                GethTransactionTraceProvider(ethereum)
            }
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(TransactionTraceProviderFactory::class.java)
    }
}
