package com.rarible.protocol.order.core.trace

import com.rarible.protocol.order.core.model.HeadTransaction
import com.rarible.protocol.order.core.model.SimpleTraceResult
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import scalether.domain.Address

interface TraceCallService {
    suspend fun findAllRequiredCalls(
        headTransaction: HeadTransaction,
        to: Address,
        vararg ids: Binary
    ): List<SimpleTraceResult>

    suspend fun findAllRequiredCallInputs(
        txHash: Word,
        txInput: Binary,
        to: Address,
        vararg ids: Binary
    ): List<Binary>

    suspend fun safeFindAllRequiredCallInputs(
        txHash: Word,
        txInput: Binary,
        to: Address,
        vararg ids: Binary
    ): List<Binary>
}
