package com.rarible.protocol.order.core.trace

import com.rarible.protocol.contracts.exchange.metatx.EIP712MetaTransaction
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.misc.methodSignatureId
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.delay
import org.springframework.stereotype.Service
import scalether.domain.Address

@Service
class TraceCallService(
    private val traceProvider: TransactionTraceProvider,
    private val featureFlags: OrderIndexerProperties.FeatureFlags,
) {

    //todo get only successfull traces
    suspend fun findAllRequiredCallInputs(txHash: Word, txInput: Binary, to: Address, id: Binary): List<Binary> {
        val metaTxSignature = EIP712MetaTransaction.executeMetaTransactionSignature()
        val realInput = if (txInput.methodSignatureId() == metaTxSignature.id()) {
            val decoded = metaTxSignature.`in`().decode(txInput, 4)
            Binary.apply(decoded.value()._2())
        } else {
            txInput
        }

        if (id == realInput.methodSignatureId()) {
            return listOf(realInput)
        } else if (featureFlags.skipGetTrace) {
            return emptyList()
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
