package com.rarible.protocol.order.core.trace

import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.NodeType
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component
import scalether.core.MonoEthereum

@Component
class TransactionTraceProviderFactory(
    private val ethereum: MonoEthereum,
    properties: OrderIndexerProperties
) {
    private val featureFlags = properties.featureFlags

    private val transactionTraceProvider: TransactionTraceProvider = runBlocking {
        val nodeVersionProvider = NodeVersionProvider(ethereum)
        val clientVersion = nodeVersionProvider.getClientVersion()
            ?: throw IllegalStateException("Can't get blockchain client version")

        when (clientVersion.type) {
            NodeType.OPEN_ETHEREUM -> {
                if (featureFlags.useCommonTransactionTraceProvider) CommonTransactionTraceProvider(ethereum)
                else OpenEthereumTransactionTraceProvider(ethereum)
            }
            NodeType.GETH, NodeType.UNKNOWN -> {
                GethTransactionTraceProvider(ethereum)
            }
        }
    }

    fun createTraceProvider(): TransactionTraceProvider {
        return transactionTraceProvider
    }
}
