package com.rarible.protocol.order.core.trace

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.rarible.protocol.order.core.model.SimpleTraceResult
import io.daonomic.rpc.RpcCodeException
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Request
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactive.awaitFirst
import org.slf4j.LoggerFactory
import scala.collection.JavaConverters
import scalether.core.MonoEthereum
import scalether.domain.Address
import scalether.java.Lists

class GethTransactionTraceProvider(
    private val ethereum: MonoEthereum
) : TransactionTraceProvider {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val mapper = ObjectMapper().apply {
        registerModule(KotlinModule())
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        setSerializationInclusion(JsonInclude.Include.NON_NULL)
    }

    override suspend fun traceAndFindCallTo(transactionHash: Word, to: Address, id: Binary): SimpleTraceResult? {
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
        val traceResult = mapper.treeToValue(result.result().get(), TraceResult::class.java)
        return traceResult.findTrace(to, id)
            ?.toSimpleTraceResult()
    }

    data class TraceResult(
        val from: Address,
        val to: Address?,
        val input: Binary,
        val calls: List<TraceResult> = emptyList()
    ) {
        fun findTrace(to: Address, id: Binary): TraceResult? {
            if (to == to && input.slice(0, 4) == id) {
                return this
            }
            return calls
                .asSequence()
                .mapNotNull { it.findTrace(to, id) }
                .firstOrNull()
        }

        fun toSimpleTraceResult(): SimpleTraceResult {
            return SimpleTraceResult(from, to, input)
        }
    }
}
