package com.rarible.protocol.order.core.trace

import com.rarible.protocol.order.core.model.SimpleTraceResult
import io.daonomic.rpc.domain.Word

interface TransactionTraceProvider {
    suspend fun getTransactionTrace(transactionHash: Word): SimpleTraceResult?
}

