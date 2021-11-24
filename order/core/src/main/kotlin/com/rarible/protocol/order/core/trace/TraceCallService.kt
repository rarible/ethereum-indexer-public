package com.rarible.protocol.order.core.trace

import com.rarible.protocol.order.core.misc.methodSignatureId
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.delay
import org.springframework.stereotype.Service
import scalether.domain.Address

@Service
class TraceCallService(
    private val traceProvider: TransactionTraceProvider
) {
    suspend fun findRequiredCallInput(txHash: Word, txInput: Binary, to: Address, id: Binary): Binary {
        if (id == txInput.methodSignatureId()) {
            return txInput
        } else {
            var attempts = 0
            do {
                val traceFound = traceProvider.traceAndFindCallTo(txHash, to, id)

                if (traceFound?.input != null) {
                    return traceFound.input
                }
                delay(200)
            } while (attempts++ < 5)
        }
        error("tx trace not found for hash: $txHash")
    }
}
