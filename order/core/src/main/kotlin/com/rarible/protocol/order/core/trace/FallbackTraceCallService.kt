package com.rarible.protocol.order.core.trace

import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.HeadTransaction
import com.rarible.protocol.order.core.model.SimpleTraceResult
import com.rarible.protocol.order.core.model.TraceMethod
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import org.springframework.stereotype.Service
import scalether.domain.Address

@Service
class FallbackTraceCallService(
    private val traceProviderFactory: TransactionTraceProviderFactory,
    private val properties: OrderIndexerProperties
) : TraceCallService {
    private val traceCallServices = createTraceCallServices()
    private val featureFlags = properties.featureFlags

    override suspend fun findAllRequiredCalls(
        headTransaction: HeadTransaction,
        to: Address,
        vararg ids: Binary
    ): List<SimpleTraceResult> {
        return findTrace(headTransaction.hash, ids, traceCallServices, featureFlags.skipGetTrace) { delegate ->
            delegate.findAllRequiredCalls(headTransaction, to, *ids)
        }
    }

    override suspend fun findAllRequiredCallInputs(
        txHash: Word,
        txInput: Binary,
        to: Address,
        vararg ids: Binary
    ): List<Binary> {
        return findTrace(txHash, ids, traceCallServices, featureFlags.skipGetTrace) { delegate ->
            delegate.findAllRequiredCallInputs(txHash, txInput, to, *ids)
        }
    }

    override suspend fun safeFindAllRequiredCallInputs(
        txHash: Word,
        txInput: Binary,
        to: Address,
        vararg ids: Binary
    ): List<Binary> {
        return findTrace(txHash, ids, traceCallServices, featureFlags.skipGetTrace, exceptionIfNotFound = false) { delegate ->
            delegate.findAllRequiredCallInputs(txHash, txInput, to, *ids)
        }
    }

    private fun createTraceCallServices(): List<TraceCallService> {
        val create: (TraceMethod) -> TraceCallService = {
            TraceCallServiceImpl(
                traceProvider = traceProviderFactory.traceProvider(it),
                featureFlags = properties.featureFlags
            )
        }
        val default = listOf(create(properties.traceMethod))
        val fallbacks = TraceMethod.values().filter { it != properties.traceMethod }.map(create)
        return default + fallbacks
    }

    private companion object {
        inline fun <T> findTrace(
            tx: Word,
            ids: Array<out Binary>,
            delegates: List<TraceCallService>,
            skipGetTrace: Boolean,
            exceptionIfNotFound: Boolean = true,
            call: (TraceCallService) -> List<T>,
        ): List<T> {
            delegates.forEach { delegate ->
                try {
                    val result = call(delegate)
                    if (result.isNotEmpty()) return result
                } catch (_: TraceNotFoundException) { }
            }
            return if (skipGetTrace) {
                emptyList()
            } else {
                if (exceptionIfNotFound) throw TraceNotFoundException("tx trace not found for hash: $tx, ids=${ids.joinToString { it.prefixed() }}") else emptyList()
            }
        }
    }
}
