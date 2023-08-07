package com.rarible.protocol.order.listener.service.descriptors.exchange.opensea

import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.core.test.data.randomAddress
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.core.model.OrderUsdValue
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.repository.nonce.NonceHistoryRepository
import com.rarible.protocol.order.core.service.ContractsProvider
import com.rarible.protocol.order.core.service.PriceNormalizer
import com.rarible.protocol.order.core.service.PriceUpdateService
import com.rarible.protocol.order.core.trace.TraceCallService
import com.rarible.protocol.order.listener.data.log
import com.rarible.protocol.order.core.metric.ForeignOrderMetrics
import com.rarible.protocol.order.listener.misc.convert
import com.rarible.protocol.order.listener.service.opensea.SeaportEventConverter
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

internal class SeaportExchangeDescriptorTest {

    private val contractsProvider = mockk<ContractsProvider>() {
        every { seaportV1() } returns listOf(randomAddress())
    }
    private val metrics: ForeignOrderMetrics = mockk {
        every { onOrderEventHandled(Platform.OPEN_SEA, "match") } returns Unit
    }

    private val traceCallService: TraceCallService = mockk()
    private val featureFlags: OrderIndexerProperties.FeatureFlags = mockk()
    private val priceUpdateService: PriceUpdateService = mockk()
    private val prizeNormalizer: PriceNormalizer = mockk()
    private val wrapperSeaportMatchEventMetric: RegisteredCounter = mockk()
    private val nonceHistoryRepository: NonceHistoryRepository = mockk()

    private val seaportEventConverter = SeaportEventConverter(
        traceCallService,
        featureFlags,
        priceUpdateService,
        prizeNormalizer,
        wrapperSeaportMatchEventMetric,
        nonceHistoryRepository
    )

    private val descriptor = SeaportV1ExchangeDescriptor(
        contractsProvider,
        seaportEventConverter,
        metrics,
        featureFlags
    )

    @BeforeEach
    fun setUp() {
        coEvery { priceUpdateService.getAssetsUsdValue(any(), any(), any()) }
            .returns(OrderUsdValue.SellOrder(BigDecimal.TEN, BigDecimal.ONE))
        coEvery { prizeNormalizer.normalize(any(), any()) }
            .returns(BigDecimal("11"))
        coEvery { prizeNormalizer.normalize(any()) }
            .returns(BigDecimal("12"))
    }

    @Test
    fun test() = runBlocking<Unit> {
        val data = "d7f041ce86f891cf278f2ea86a7e6314d379e0338ce56e06bc958edae47dabd0" +
            "000000000000000000000000be7c3c0973d0c25e9e689a59ee296abd2546130d" +
            "0000000000000000000000000000000000000000000000000000000000000080" +
            "0000000000000000000000000000000000000000000000000000000000000120" +
            "0000000000000000000000000000000000000000000000000000000000000001" +
            "0000000000000000000000000000000000000000000000000000000000000002" +
            "00000000000000000000000059325733eb952a92e069c87f0a6168b29e80627f" +
            "00000000000000000000000000000000000000000000000000000000000006f6" +
            "0000000000000000000000000000000000000000000000000000000000000001" +
            "0000000000000000000000000000000000000000000000000000000000000002" +
            "0000000000000000000000000000000000000000000000000000000000000000" +
            "0000000000000000000000000000000000000000000000000000000000000000" +
            "0000000000000000000000000000000000000000000000000000000000000000" +
            "000000000000000000000000000000000000000000000000131ddcf3a219c000" +
            "0000000000000000000000000dfd32320af10f58cd4a5c2b567b7739ea2d691c" +
            "0000000000000000000000000000000000000000000000000000000000000000" +
            "0000000000000000000000000000000000000000000000000000000000000000" +
            "0000000000000000000000000000000000000000000000000000000000000000" +
            "0000000000000000000000000000000000000000000000000101925daa374000" +
            "000000000000000000000000a1640edd7b69a3bdf98cd9a6a61f663dcf6d2aa2"
        val transactionInput = Binary.apply(
            "0xfb0f3ee1000000000000000000000000000000000000000000000000000000000000002000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000131ddcf3a219c0000000000000000000000000000dfd32320af10f58cd4a5c2b567b7739ea2d691c000000000000000000000000004c00500000ad104d7dbd00e3ae0a5c00560c0000000000000000000000000059325733eb952a92e069c87f0a6168b29e80627f00000000000000000000000000000000000000000000000000000000000006f6000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000064068295000000000000000000000000000000000000000000000000000000006407d3200000000000000000000000000000000000000000000000000000000000000000360c6ebe00000000000000000000000000000000000000007de6ae636f9223ad0000007b02230091a7ed01230072f7006a004d60a8d4e71d599b8104250f000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000024000000000000000000000000000000000000000000000000000000000000002a000000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000101925daa374000000000000000000000000000a1640edd7b69a3bdf98cd9a6a61f663dcf6d2aa200000000000000000000000000000000000000000000000000000000000000409de01118c5285a5ec838d86ea8aaaeff86f6f3ee0dd1545b096d7ebd0bc1d46573ded3a67992e9bcc4814c758e2f201c8cd33ccac9a41297abd788e5ca3706a61f6e66746e657264732e61691f"
        )
        val date = Instant.ofEpochSecond(1)
        val log = log(
            topics = listOf(
                Word.apply("0x9d9af8e38d66c62e2c12f0225249fd9d721c54b83f48d9352c97c6cacdcb6f31"),
                Word.apply("0x0000000000000000000000000dfd32320af10f58cd4a5c2b567b7739ea2d691c"),
                Word.apply("0x000000000000000000000000004c00500000ad104d7dbd00e3ae0a5c00560c00")
            ),
            data = data
        )
        descriptor.convert<OrderSideMatch>(log, transactionInput, date.epochSecond, 1, 3)
        verify(exactly = 1) { metrics.onOrderEventHandled(Platform.OPEN_SEA, "match") }
    }
}
