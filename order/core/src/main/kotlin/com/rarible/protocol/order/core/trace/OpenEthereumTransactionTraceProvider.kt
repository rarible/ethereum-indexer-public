package com.rarible.protocol.order.core.trace

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.rarible.protocol.order.core.model.SimpleTraceResult
import io.daonomic.rpc.domain.Request
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactive.awaitFirst
import org.slf4j.LoggerFactory
import scalether.core.MonoEthereum
import scalether.domain.Address
import scalether.java.Lists

class OpenEthereumTransactionTraceProvider(
    private val ethereum: MonoEthereum
) : TransactionTraceProvider {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val mapper = ObjectMapper().apply {
        registerModule(KotlinModule())
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        setSerializationInclusion(JsonInclude.Include.NON_NULL)
    }

    override suspend fun getTransactionTrace(transactionHash: Word): SimpleTraceResult? {
        val result = ethereum.executeRaw(Request(1, "trace_replayTransaction", Lists.toScala(
            transactionHash.toString(),
            Lists.toScala("trace", "stateDiff")
        ), "2.0")).awaitFirst()

        return result.result()
            .map { mapper.treeToValue(it, OpenEthereumTrace::class.java) }
            .map { convert(it) }
            .getOrElse {
                if (result.error().isEmpty.not()) {
                    logger.error("Can't fetch trace: {}", result.error().get())
                }
                null
            }
    }

    private fun convert(source: OpenEthereumTrace): SimpleTraceResult? {
        return source.trace.firstOrNull()?.let { trace ->
            SimpleTraceResult(
                type = trace.action.callType,
                from = trace.action.from,
                to = trace.action.to,
                input = trace.action.input,
                output = trace.result?.output ?: "",
                valueHexString = trace.action.value
            )
        }
    }

    private data class OpenEthereumTrace(
        val trace: List<Trace>
    ) {
        data class Trace(
            val action: Action,
            val result: Result?
        ) {
            data class Action(
                val callType: String,
                val from: Address,
                val to: Address,
                val input: String,
                val value: String
            )

            data class Result(
                val output: String
            )
        }
    }
}
