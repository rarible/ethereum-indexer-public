package com.rarible.protocol.order.core.trace

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.order.core.model.SimpleTraceResult
import io.daonomic.rpc.domain.Word

@CaptureSpan(type = SpanType.EXT)
interface TransactionTraceProvider {
    suspend fun getTransactionTrace(transactionHash: Word): SimpleTraceResult?
}

