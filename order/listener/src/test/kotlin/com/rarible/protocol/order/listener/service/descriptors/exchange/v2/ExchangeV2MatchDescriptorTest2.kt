package com.rarible.protocol.order.listener.service.descriptors.exchange.v2

import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.core.test.data.randomAddress
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.data.randomSimpleTrace
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.core.service.ContractsProvider
import com.rarible.protocol.order.core.service.PriceNormalizer
import com.rarible.protocol.order.core.service.PriceUpdateService
import com.rarible.protocol.order.core.service.RaribleExchangeV2OrderParser
import com.rarible.protocol.order.core.trace.TraceCallServiceImpl
import com.rarible.protocol.order.core.trace.TransactionTraceProvider
import com.rarible.protocol.order.listener.data.log
import com.rarible.protocol.order.listener.misc.convert
import com.rarible.protocol.order.listener.service.descriptors.AutoReduceService
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import scalether.domain.Address
import scalether.domain.response.Transaction
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit

class ExchangeV2MatchDescriptorTest2 {
    private val priceUpdateService = mockk<PriceUpdateService>()
    private val priceNormalizer = mockk<PriceNormalizer>()
    private val raribleMatchEventMetric = mockk<RegisteredCounter>()
    private val featureFlags = mockk<OrderIndexerProperties.FeatureFlags>()
    private val contractsProvider = mockk<ContractsProvider> {
        every { raribleExchangeV2() } returns listOf(randomAddress())
    }
    private val exchangeContractAddresses = mockk<OrderIndexerProperties.ExchangeContractAddresses>()
    private val transactionTraceProvider = mockk<TransactionTraceProvider>()
    private val traceCallService = TraceCallServiceImpl(
        transactionTraceProvider,
        OrderIndexerProperties.FeatureFlags()
    )
    private val raribleOrderParser = RaribleExchangeV2OrderParser(exchangeContractAddresses, traceCallService)
    private val autoReduceService = mockk<AutoReduceService>()
    private val descriptor = ExchangeOrderMatchDescriptor(
        contractsProvider,
        priceUpdateService,
        priceNormalizer,
        raribleOrderParser,
        raribleMatchEventMetric,
        featureFlags,
        autoReduceService,
    )

    @Test
    fun name() = runBlocking<Unit> {
        val transaction = mockk<Transaction>()
        coEvery { transaction.hash() }.returns(Word.apply("0x01688763b0156ce1dad5620235697a4589f79946dedb65b3f63dbb4d3528c426"))
        coEvery { transaction.input() }.returns(
            Binary.apply(
                "0x0d5f7d3500000000000000000000000000000000000000000000000000000000000000200000000000000000000000001fe2d5b7fbf6dbb014ba664c1df8a0692c5295df000000000000000000000000000000000000000000000000000000000000000173ad21460000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001e0000000000000000000000000000000000000000000000000002386f26fc1000000000000000000000000000000000000000000000000000000000000000000009da35d65c1e74166e58eb597ae4f83bd0d5f82eeb918f8e87f6ac079ac6c2a840000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000023d235ef0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000002400000000000000000000000000000000000000000000000000000000000000320000000000000000000000000000000000000000000000000002386f26fc10000000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000003a00000000000000000000000000000000000000000000000000000000000000040000000000000000000000000261352dbe9914f560a419044eaecf9e9901f80cb000000000000000000000000000000000000000000000000000000000000000200000000000000000000000000000000000000000000000000000000000000c00000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000006000000000000000000000000000000000000000000000000000000000000000800000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000041c5d38d859bfe5d681e34261ba81b69c056d4b24ad4b51d0adc1c3230b0fd013775a2d6e554b76abfb0f54181fde5feba93137a3df3b35d8fd2f697f03344888f1c0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000c000000000000000000000000000000000000000000000000000000000000000200000000000000000000000000000000000000000000000000000000000000060000000000000000000000000000000000000000000000000000000000000008000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000009616c6c64617461"
            )
        )
        coEvery { transaction.from() }.returns(Address.FOUR())
        coEvery { transaction.to() }.returns(Address.ONE())
        coEvery {
            transactionTraceProvider.traceAndFindAllCallsTo(
                eq(transaction.hash()),
                any(),
                any()
            )
        } returns listOf(
            randomSimpleTrace()
        )
        coEvery { priceNormalizer.normalize(any()) }.returns(BigDecimal.ONE)
        coEvery { exchangeContractAddresses.v2 }.returns(Address.ONE())
        coEvery { priceUpdateService.getAssetsUsdValue(any(), any(), any()) }.returns(null)
        coEvery { raribleMatchEventMetric.increment(any()) }.answers { }
        val data = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val log = log(
            listOf(
                Word.apply("0x956cd63ee4cdcd81fda5f0ec7c6c36dceda99e1b412f4a650a5d26055dc3c450"),
            ),
            "0xf765978d32c80537ba009141cc0e96bec3e0292b33cade67bb6ba373b03e129e4a6b3ac393ff1b19d7fe895a75c03af2e27ea92117ea8eaec291957bbe0a9773000000000000000000000000000000000000000000000000002386f26fc100000000000000000000000000000000000000000000000000000000000000000001"
        )
        val matches = descriptor.convert<OrderSideMatch>(log, transaction, data.epochSecond, 0, 1)
        println(matches)
    }
}
