package com.rarible.protocol.order.core.trace

import com.github.michaelbull.retry.policy.constantDelay
import com.github.michaelbull.retry.policy.limitAttempts
import com.github.michaelbull.retry.policy.plus
import com.github.michaelbull.retry.retry
import com.rarible.protocol.order.core.misc.methodSignatureId
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import org.springframework.stereotype.Service
import scalether.domain.Address

@Service
class TraceCallService(
    private val traceProvider: TransactionTraceProvider
) {
    suspend fun findRequiredCallInput(txHash: Word, txInput: Binary, to: Address, id: Binary): Binary {
        return if (id == txInput.methodSignatureId()) {
            txInput
        } else {
            retry(limitAttempts(5) + constantDelay(200)) {
                val traceFound = traceProvider.traceAndFindCallTo(txHash, to, id)
                traceFound?.input ?: error("tx trace not found for hash: $txHash")
            }
        }
    }
}