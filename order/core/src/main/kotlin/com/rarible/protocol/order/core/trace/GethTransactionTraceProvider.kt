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
import reactor.kotlin.extra.retry.retryExponentialBackoff
import scala.collection.JavaConverters
import scalether.core.MonoEthereum
import scalether.java.Lists
import java.time.Duration

class GethTransactionTraceProvider(
    private val ethereum: MonoEthereum
) : TransactionTraceProvider {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val mapper = ObjectMapper().apply {
        registerModule(KotlinModule())
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        setSerializationInclusion(JsonInclude.Include.NON_NULL)
    }

    override suspend fun getTransactionTrace(transactionHash: Word): SimpleTraceResult? {
        val result = ethereum.executeRaw(
            Request(
                1, "debug_traceTransaction", Lists.toScala(
                    transactionHash.toString(),
                    JavaConverters.asScala(mapOf("tracer" to "callTracer"))
                ), "2.0"
            )
        ).retryExponentialBackoff(
            times = 10,
            first = Duration.ofMillis(100),
            max = Duration.ofMinutes(2),
            jitter = true,
            doOnRetry = {
                logger.warn("Failed attempt #${it.iteration()} to get Geth transaction trace: ${it.exception()?.message}")
            }
        ).awaitFirst()

        return result.result()
            .map { mapper.treeToValue(it, SimpleTraceResult::class.java) }
            .getOrElse {
                if (result.error().isEmpty.not()) {
                    logger.error("Can't fetch trace: {}", result.error().get())
                }
                null
            }
    }
}
