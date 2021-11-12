package com.rarible.protocol.order.core.trace

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.treeToValue
import com.rarible.protocol.order.core.model.SimpleTraceResult
import io.daonomic.rpc.domain.Request
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.delay
import kotlinx.coroutines.reactive.awaitFirst
import org.slf4j.LoggerFactory
import scalether.core.MonoEthereum
import scalether.domain.Address
import scalether.java.Lists
import kotlin.math.pow

class CommonTransactionTraceProvider(
    private val ethereum: MonoEthereum
) : TransactionTraceProvider {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val mapper = ObjectMapper().apply {
        registerModule(KotlinModule())
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        setSerializationInclusion(JsonInclude.Include.NON_NULL)
    }

    override suspend fun getTransactionTrace(transactionHash: Word): SimpleTraceResult {
        try {
            val request = Request(1, "trace_transaction", Lists.toScala(transactionHash.toString()), "2.0")
            val attempts = 8
            for (attempt in 0 until attempts) {
                try {
                    val response = ethereum.executeRaw(request).awaitFirst()
                    val result: JsonNode? = response.result().getOrElse { null }
                    val trace = result
                        ?.let { mapper.treeToValue<Array<Trace>>(it) }
                        ?.let { convertTraces(it) }
                    if (trace != null) {
                        return trace
                    }
                    if (response.error().isEmpty.not()) {
                        val errorMessage = response.error().get().fullMessage()
                        logger.error("Failed attempt $attempt/$attempts to fetch trace of $transactionHash: $errorMessage")
                    }
                } catch (e: Throwable) {
                    logger.error("Error attempt $attempt/$attempts to fetch trace of $transactionHash", e)
                }
                delay(200 * 2.0.pow(attempt.toDouble() + 1).toLong())
            }
            error("Failed to fetch trace by hash $transactionHash in $attempts attempts")
        } catch (ex: Throwable) {
            logger.error("Can't fetch trace by hash $transactionHash")
            throw ex
        }
    }

    private fun convertTraces(source: Array<Trace>): SimpleTraceResult? {
        val trace = source.firstOrNull() ?: return null
        val action = trace.action
        if (action.callType != null) {
            val type = action.callType
            val from = requireNotNull(action.from) { "From can't be null" }
            val to = requireNotNull(action.to) { "To can't be null" }
            val input = requireNotNull(action.input) { "Input can't be null" }
            val valueHexString = requireNotNull(action.value) { "Value can't be null" }
            val output = trace.result?.output
            return SimpleTraceResult(
                type = type,
                from = from,
                to = to,
                input = input,
                output = output,
                valueHexString = valueHexString
            )
        } else if (trace.action.address != null) {
            return null
        } else {
            throw IllegalArgumentException("Unsupported trace type $trace")
        }
    }

    data class Trace(
        val action: Action,
        val result: Result?
    ) {
        data class Action(
            val callType: String?,
            val from: Address?,
            val to: Address?,
            val input: String?,
            val value: String?,
            val address: Address?,
            val balance: String?,
            val refundAddress: Address?
        )

        data class Result(
            val output: String?
        )
    }
}
