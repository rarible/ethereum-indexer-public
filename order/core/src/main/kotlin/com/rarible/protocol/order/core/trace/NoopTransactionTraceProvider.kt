package com.rarible.protocol.order.core.trace

import com.rarible.protocol.order.core.model.SimpleTraceResult
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import scalether.domain.Address

class NoopTransactionTraceProvider : TransactionTraceProvider {
    override suspend fun traceAndFindAllCallsTo(transactionHash: Word, to: Address, ids: Set<Binary>): List<SimpleTraceResult> {
        return emptyList()
    }
}
