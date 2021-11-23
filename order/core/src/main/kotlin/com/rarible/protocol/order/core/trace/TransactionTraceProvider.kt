package com.rarible.protocol.order.core.trace

import com.rarible.protocol.order.core.model.SimpleTraceResult
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import scalether.domain.Address

interface TransactionTraceProvider {
    /**
     * Finds call to specific contract with specific identifier
     */
    suspend fun traceAndFindCallTo(transactionHash: Word, to: Address, id: Binary): SimpleTraceResult?
}

