package com.rarible.protocol.order.core.trace

import com.rarible.protocol.contracts.exchange.wyvern.WyvernExchange
import com.rarible.protocol.order.core.misc.methodSignatureId
import com.rarible.protocol.order.core.service.CallDataEncoder
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.delay
import org.springframework.stereotype.Service
import scalether.domain.Address

@Service
class TraceCallService(
    private val traceProvider: TransactionTraceProvider,
    private var callDataEncoder: CallDataEncoder
) {

    //todo get only successfull traces
    suspend fun findAllRequiredCallInputs(txHash: Word, txInput: Binary, to: Address, id: Binary): List<Binary> {
        if (id == txInput.methodSignatureId() && !isDelegateAtomicSwapCall(id, txInput)) {
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

    fun isDelegateAtomicSwapCall(id: Binary, txInput: Binary): Boolean =
        if (id == txInput.methodSignatureId()) {
            val signature = WyvernExchange.atomicMatch_Signature()
            val decoded = signature.`in`().decode(txInput, 4)
            val buyMaker: Address = decoded.value()._1()[1]
            val callDataBuy = Binary.apply(decoded.value()._4())
            val buyMakerCallData: Address = callDataEncoder.decodeTransfer(callDataBuy).to
            buyMaker != buyMakerCallData
        } else {
            false
        }
}
