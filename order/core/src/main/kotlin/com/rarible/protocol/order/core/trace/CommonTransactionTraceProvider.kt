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

class CommonTransactionTraceProvider(
    private val ethereum: MonoEthereum
) : TransactionTraceProvider {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val mapper = ObjectMapper().apply {
        registerModule(KotlinModule())
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        setSerializationInclusion(JsonInclude.Include.NON_NULL)
    }

    override suspend fun getTransactionTrace(transactionHash: Word): SimpleTraceResult? {
        try {
            val result = ethereum.executeRaw(
                Request(1, "trace_transaction", Lists.toScala(transactionHash.toString()), "2.0")
            ).awaitFirst()

            return result.result()
                .map { mapper.treeToValue(it, Array<Trace>::class.java) }
                .map { convert(it) }
                .getOrElse {
                    if (result.error().isEmpty.not()) {
                        logger.error("Can't fetch trace: {}", result.error().get())
                    }
                    null
                }
        } catch (ex: Throwable) {
            logger.error("Can't fetch trace by hash $transactionHash")
            throw ex
        }
    }

    private fun convert(source: Array<Trace>): SimpleTraceResult? {
        return source.firstOrNull()?.let { trace ->
            val action = trace.action
            if (action.callType != null) {
                val type = action.callType
                val from = action.from ?: throw IllegalArgumentException("From can't be null")
                val to = action.to ?: throw IllegalArgumentException("To can't be null")
                val input = action.input ?: throw IllegalArgumentException("Input can't be null")
                val output = trace.result?.output ?: "0x"

                SimpleTraceResult(
                    type = type,
                    from = from,
                    to = to,
                    input = input,
                    output = output
                )
            } else if (trace.action.address != null) {
                null
            } else {
                throw IllegalArgumentException("Unsupported trace type $trace")
            }
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
            val address: Address?,
            val balance: String?,
            val refundAddress: Address?
        )

        data class Result(
            val output: String
        )
    }
}
