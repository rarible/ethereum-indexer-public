package com.rarible.protocol.order.core.trace

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBinary
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.data.randomHeadTransaction
import com.rarible.protocol.order.core.data.randomSimpleTrace
import com.rarible.protocol.order.core.model.TraceMethod
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

internal class FallbackTraceCallServiceTest {
    private val defaultProvider = mockk<TransactionTraceProvider>()
    private val otherProvider = mockk<TransactionTraceProvider>()
    private val traceProviderFactory = mockk<TransactionTraceProviderFactory> {
        every { traceProvider(TraceMethod.TRACE_TRANSACTION) } returns defaultProvider
        every { traceProvider(TraceMethod.DEBUG_TRACE_TRANSACTION) } returns otherProvider
    }
    private val properties = mockk<OrderIndexerProperties> {
        every { traceMethod } returns TraceMethod.TRACE_TRANSACTION
        every { featureFlags } returns OrderIndexerProperties.FeatureFlags()
    }
    private val fallbackTraceCallService = FallbackTraceCallService(traceProviderFactory, properties)

    @Test
    fun `should use default TraceCallService from config`() = runBlocking<Unit> {
        val simpleTrace = randomSimpleTrace()
        val headTransaction = randomHeadTransaction()
        coEvery { defaultProvider.traceAndFindAllCallsTo(eq(headTransaction.hash), any(), any()) } returns listOf(simpleTrace)

        fallbackTraceCallService.findAllRequiredCalls(headTransaction, randomAddress(), randomBinary())

        coVerify(exactly = 1) { defaultProvider.traceAndFindAllCallsTo(any(), any(), any()) }
        coVerify(exactly = 0) { otherProvider.traceAndFindAllCallsTo(any(), any(), any()) }
    }

    @Test
    fun `should use other TraceCallService`() = runBlocking<Unit> {
        val headTransaction = randomHeadTransaction()
        val simpleTrace = randomSimpleTrace()
        coEvery { defaultProvider.traceAndFindAllCallsTo(eq(headTransaction.hash), any(), any()) } returns emptyList()
        coEvery { otherProvider.traceAndFindAllCallsTo(eq(headTransaction.hash), any(), any()) } returns listOf(simpleTrace)

        fallbackTraceCallService.findAllRequiredCalls(headTransaction, randomAddress(), randomBinary())

        coVerify(atLeast = 1) { defaultProvider.traceAndFindAllCallsTo(any(), any(), any()) }
        coVerify(exactly = 1) { otherProvider.traceAndFindAllCallsTo(any(), any(), any()) }
    }
}
