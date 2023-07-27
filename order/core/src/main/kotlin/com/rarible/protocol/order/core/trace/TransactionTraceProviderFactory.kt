package com.rarible.protocol.order.core.trace

import com.rarible.protocol.order.core.model.TraceMethod
import org.springframework.stereotype.Service
import scalether.core.MonoEthereum

@Service
class TransactionTraceProviderFactory(private val ethereum: MonoEthereum) {
    fun traceProvider(method: TraceMethod): TransactionTraceProvider {
        return when (method) {
            TraceMethod.TRACE_TRANSACTION -> {
                OpenEthereumTransactionTraceProvider(ethereum)
            }
            TraceMethod.DEBUG_TRACE_TRANSACTION -> {
                GethTransactionTraceProvider(ethereum)
            }
        }
    }
}
