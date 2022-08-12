@file:Suppress("DEPRECATION")

package com.rarible.protocol.order.core.trace

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.rarible.protocol.order.core.misc.methodSignatureId
import com.rarible.protocol.order.core.model.SimpleTraceResult
import io.daonomic.rpc.RpcCodeException
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Request
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactive.awaitFirst
import scala.collection.JavaConverters
import scalether.core.MonoEthereum
import scalether.domain.Address
import scalether.java.Lists

class GethTransactionTraceProvider(
    private val ethereum: MonoEthereum
) : TransactionTraceProvider {

    private val mapper = ObjectMapper().apply {
        registerModule(KotlinModule())
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        setSerializationInclusion(JsonInclude.Include.NON_NULL)
    }

    override suspend fun traceAndFindAllCallsTo(
        transactionHash: Word,
        to: Address,
        ids: Set<Binary>,
    ): List<SimpleTraceResult> {
        return trace(transactionHash).findTraces(to, ids).map { it.toSimpleTraceResult() }
    }

    suspend fun trace(transactionHash: Word): TraceResult {
        val result = ethereum.executeRaw(
            Request(
                1, "debug_traceTransaction", Lists.toScala(
                    transactionHash.toString(),
                    JavaConverters.asScala(mapOf("tracer" to "callTracer"))
                ), "2.0"
            )
        ).awaitFirst()

        if (result.error().isDefined) {
            val error = result.error().get()
            throw RpcCodeException("Unable to get trace", error)
        }

        if (result.result().isEmpty) {
            error("Trace result not found")
        }

        @Suppress("BlockingMethodInNonBlockingContext")
        return mapper.treeToValue(result.result().get(), TraceResult::class.java)
    }

    data class TraceResult(
        val from: Address,
        val to: Address?,
        val input: Binary,
        val calls: List<TraceResult> = emptyList()
    ) {
        private fun findTrace(to: Address, ids: Set<Binary>): TraceResult? {
            if (this.to == to && input.methodSignatureId() in ids) {
                return this
            }
            return calls
                .asSequence()
                .mapNotNull { it.findTrace(to, ids) }
                .firstOrNull()
        }

        fun findTraces(to: Address, ids: Set<Binary>): List<TraceResult> {
            return calls.mapNotNull { it.findTrace(to, ids) }
        }

        fun toSimpleTraceResult(): SimpleTraceResult {
            return SimpleTraceResult(from, to, input)
        }
    }
}
