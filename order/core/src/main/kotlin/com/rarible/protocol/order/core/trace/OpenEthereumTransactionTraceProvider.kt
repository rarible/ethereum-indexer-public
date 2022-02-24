package com.rarible.protocol.order.core.trace

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.treeToValue
import com.rarible.protocol.order.core.misc.methodSignatureId
import com.rarible.protocol.order.core.model.SimpleTraceResult
import io.daonomic.rpc.RpcCodeException
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Request
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactive.awaitFirst
import scalether.core.MonoEthereum
import scalether.domain.Address
import scalether.java.Lists

class OpenEthereumTransactionTraceProvider(
    private val ethereum: MonoEthereum
) : TransactionTraceProvider {
    private val mapper = ObjectMapper().apply {
        registerModule(KotlinModule())
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        setSerializationInclusion(JsonInclude.Include.NON_NULL)
    }

    override suspend fun traceAndFindFirstCallTo(transactionHash: Word, to: Address, id: Binary): SimpleTraceResult? {
        return traces(transactionHash)
            .asSequence()
            .filter { it.action?.to == to && it.action.input?.methodSignatureId() == id }
            .mapNotNull { convert(it) }
            .firstOrNull()
    }

    override suspend fun traceAndFindAllCallsTo(
        transactionHash: Word,
        to: Address,
        id: Binary
    ): List<SimpleTraceResult> {
        return traces(transactionHash)
            .filter { it.action?.to == to && it.action.input?.methodSignatureId() == id && it.error != "Reverted" }
            .mapNotNull { convert(it) }
    }

    private suspend fun traces(transactionHash: Word): Array<Trace> {
        val request = Request(1, "trace_transaction", Lists.toScala(transactionHash.toString()), "2.0")
        val result = ethereum.executeRaw(request).awaitFirst()

        if (result.error().isDefined) {
            val error = result.error().get()
            throw RpcCodeException("Unable to get trace", error)
        }

        if (result.result().isEmpty) {
            error("Trace result not found")
        }

        @Suppress("BlockingMethodInNonBlockingContext")
        return mapper.treeToValue<Array<Trace>>(result.result().get())!!
    }

    private fun convert(trace: Trace): SimpleTraceResult? {
        return if (trace.action?.from != null) {
            SimpleTraceResult(
                from = trace.action.from,
                to = trace.action.to,
                input = trace.action.input ?: Binary.empty()
            )
        } else {
            null
        }
    }

    data class Trace(
        val action: Action?,
        val error: String?,
        val result: Result?
    ) {
        data class Action(
            val callType: String?,
            val from: Address?,
            val to: Address?,
            val input: Binary?,
            val value: String?
        )

        data class Result(
            val output: String?
        )
    }
}
