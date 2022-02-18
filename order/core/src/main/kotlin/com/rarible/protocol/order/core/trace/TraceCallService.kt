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

    //todo get only successfull traces
    suspend fun findAllRequiredCallInputs(txHash: Word, txInput: Binary, to: Address, id: Binary): List<Binary> {
        if (id == txInput.methodSignatureId()) {
            return listOf(txInput)
        } else {
            var attempts = 0
            do {
                val tracesFound = traceProvider.traceAndFindAllCallsTo(txHash, to, id)
                if (tracesFound.isNotEmpty()) {
                    return tracesFound.map { it.input }
                }
                delay(200)
            } while (attempts++ < 5)
        }
        error("tx trace not found for hash: $txHash")
    }

}
