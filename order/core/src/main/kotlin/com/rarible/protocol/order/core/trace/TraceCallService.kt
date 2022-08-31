package com.rarible.protocol.order.core.trace

import com.rarible.protocol.contracts.exchange.metatx.EIP712MetaTransaction
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.misc.methodSignatureId
import com.rarible.protocol.order.core.model.HeadTransaction
import com.rarible.protocol.order.core.model.SimpleTraceResult
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.delay
import org.springframework.stereotype.Service
import scalether.domain.Address
import java.math.BigInteger

@Service
class TraceCallService(
    private val traceProvider: TransactionTraceProvider,
    private val featureFlags: OrderIndexerProperties.FeatureFlags,
) {
    //todo get only success traces
    suspend fun findAllRequiredCalls(
        headTransaction: HeadTransaction,
        to: Address,
        vararg ids: Binary
    ): List<SimpleTraceResult> {
        val set = ids.toSet()
        val txHash = headTransaction.hash
        val txInput = headTransaction.input

        val metaTxSignature = EIP712MetaTransaction.executeMetaTransactionSignature()
        val realInput = if (txInput.methodSignatureId() == metaTxSignature.id()) {
            val decoded = metaTxSignature.`in`().decode(txInput, 4)
            Binary.apply(decoded.value()._2())
        } else {
            txInput
        }
        if (realInput.methodSignatureId() in set) {
            return listOf(
                SimpleTraceResult(
                    from = headTransaction.from,
                    to = headTransaction.to,
                    value = headTransaction.value,
                    input = realInput,
                )
            )
        } else if (featureFlags.skipGetTrace) {
            return emptyList()
        } else {
            var attempts = 0
            do {
                val tracesFound = traceProvider.traceAndFindAllCallsTo(txHash, to, set)
                if (tracesFound.isNotEmpty()) {
                    return tracesFound
                }
                delay(200)
            } while (attempts++ < 5)
        }
        error("tx trace not found for hash: $txHash")
    }

    suspend fun findAllRequiredCallInputs(
        txHash: Word,
        txInput: Binary,
        to: Address,
        vararg ids: Binary
    ): List<Binary> {
        return findAllRequiredCalls(
            headTransaction = HeadTransaction(
                hash = txHash,
                input = txInput,
                to = Address.ZERO(),
                from = Address.ZERO(),
                value = BigInteger.ZERO
            ),
            to = to,
            ids = ids
        ).map { it.input }
    }
}
