package com.rarible.protocol.order.listener.service.descriptors.exchange.looksrare

import com.rarible.core.contract.model.Erc20Token
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.core.test.data.randomAddress
import com.rarible.ethereum.contract.service.ContractService
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.data.randomBidOrderUsdValue
import com.rarible.protocol.order.core.data.randomSellOrderUsdValue
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.OrderSide
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.model.TokenStandard
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.ContractsProvider
import com.rarible.protocol.order.core.service.PriceNormalizer
import com.rarible.protocol.order.core.service.PriceUpdateService
import com.rarible.protocol.order.listener.data.log
import com.rarible.protocol.order.listener.misc.ForeignOrderMetrics
import com.rarible.protocol.order.listener.misc.convert
import com.rarible.protocol.order.listener.service.looksrare.TokenStandardProvider
import io.daonomic.rpc.domain.Word
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import scalether.domain.Address
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit

internal class LooksrareV2ExchangeTakerDescriptorTest {

    private val contractsProvider = mockk<ContractsProvider> {
        every { looksrareV2() } returns listOf(randomAddress())
        every { weth() } returns Address.apply("0xc778417e063141139fce010982780140aa0cd5ab")
    }
    private val wrapperLooksrareMetric = mockk<RegisteredCounter> {
        every { increment() } returns Unit
    }
    private val metrics = mockk<ForeignOrderMetrics>() {
        every { onOrderEventHandled(Platform.LOOKSRARE, any()) } returns Unit
    }
    private val tokenStandardProvider = mockk<TokenStandardProvider>()
    private val priceUpdateService = mockk<PriceUpdateService>()
    private val contractService = mockk<ContractService>()
    private val orderRepository = mockk<OrderRepository>()
    private val prizeNormalizer = PriceNormalizer(contractService)

    private val descriptorAsk = LooksrareV2ExchangeTakerAskDescriptor(
        contractsProvider,
        orderRepository,
        wrapperLooksrareMetric,
        tokenStandardProvider,
        priceUpdateService,
        prizeNormalizer,
        metrics
    )
    private val descriptorBid = LooksrareV2ExchangeTakerBidDescriptor(
        contractsProvider,
        orderRepository,
        wrapperLooksrareMetric,
        tokenStandardProvider,
        priceUpdateService,
        prizeNormalizer,
        metrics
    )

    @Test
    fun `should convert sell event`() = runBlocking<Unit> {
        val data = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val bidOrderUsd = randomBidOrderUsdValue()
        val sellOrderUsd = randomSellOrderUsdValue()
        val log = log(
            listOf(
                Word.apply("0x3ee3de4684413690dee6fff1a0a4f92916a1b97d1c5a83cdf24671844306b2e3"),
            ),
            "1855813a4636aa7dc4078f1412c483264e1b0ff45c3b90910b5800669f242c2c" +
                    "0000000000000000000000000000000000000000000000000000000000000000" +
                    "0000000000000000000000000000000000000000000000000000000000000001" +
                    "0000000000000000000000006a67e811868e54f7d4547f5feba0be8a043f7837" +
                    "0000000000000000000000006a67e811868e54f7d4547f5feba0be8a043f7837" +
                    "0000000000000000000000000000000000000000000000000000000000000000" +
                    "0000000000000000000000000000000000000000000000000000000000000000" +
                    "000000000000000000000000edb61f74b0d09b2558f1eeb79b247c1f363ae452" +
                    "00000000000000000000000000000000000000000000000000000000000001e0" +
                    "0000000000000000000000000000000000000000000000000000000000000220" +
                    "000000000000000000000000317a690c61951e0b05a4bd65fabf483f98de44cd" +
                    "0000000000000000000000000000000000000000000000000000000000000000" +
                    "0000000000000000000000000000000000000000000000000b0bf5c859360000" +
                    "0000000000000000000000000000000000000000000000000000000000000000" +
                    "000000000000000000000000000000000000000000000000000e35fa931a0000" +
                    "0000000000000000000000000000000000000000000000000000000000000001" +
                    "0000000000000000000000000000000000000000000000000000000000000b10" +
                    "0000000000000000000000000000000000000000000000000000000000000001" +
                    "0000000000000000000000000000000000000000000000000000000000000001"
        )
        val nftAssetType = Erc721AssetType(
            Address.apply("0xedb61f74b0d09b2558f1eeb79b247c1f363ae452"),
            EthUInt256.of("2832")
        )
        val expectedNft = Asset(
            nftAssetType,
            EthUInt256.ONE
        )
        val expectedPayment = Asset(
            EthAssetType,
            EthUInt256.of("796000000000000000")
        )
        coEvery {
            priceUpdateService.getAssetsUsdValue(make = expectedNft, take = expectedPayment, at = data)
        } returns sellOrderUsd

        coEvery {
            priceUpdateService.getAssetsUsdValue(make = expectedPayment, take = expectedNft, at = data)
        } returns bidOrderUsd

        coEvery { tokenStandardProvider.getTokenStandard(nftAssetType.token) } returns TokenStandard.ERC721
        coEvery { orderRepository.findByMakeAndByCounters(any(), any(), any()) } returns emptyFlow()

        val matches = descriptorBid.convert<OrderSideMatch>(log, data.epochSecond, 0, 1)

        assertThat(matches).hasSize(2)
        val left = matches[0] as OrderSideMatch
        assertThat(left.hash).isEqualTo(Word.apply("1855813a4636aa7dc4078f1412c483264e1b0ff45c3b90910b5800669f242c2c"))
        assertThat(left.side).isEqualTo(OrderSide.LEFT)
        assertThat(left.maker).isEqualTo(Address.apply("0x317a690C61951E0b05A4Bd65FaBf483f98de44cd"))
        assertThat(left.taker).isEqualTo(Address.apply("0x6a67E811868e54f7d4547F5FeBA0bE8A043f7837"))
        assertThat(left.make).isEqualTo(expectedNft)
        assertThat(left.take).isEqualTo(expectedPayment)
        assertThat(left.fill).isEqualTo(expectedNft.value)
        assertThat(left.date).isEqualTo(data)
        assertThat(left.makeUsd).isEqualTo(sellOrderUsd.makeUsd)
        assertThat(left.takeUsd).isEqualTo(sellOrderUsd.takeUsd)
        assertThat(left.makePriceUsd).isEqualTo(sellOrderUsd.makePriceUsd)
        assertThat(left.takePriceUsd).isEqualTo(sellOrderUsd.takePriceUsd)
        assertThat(left.takeValue).isEqualTo(BigDecimal("0.796000000000000000"))
        assertThat(left.makeValue).isEqualTo(BigDecimal.ONE)
        assertThat(left.source).isEqualTo(HistorySource.LOOKSRARE)
        assertThat(left.adhoc).isFalse()
        assertThat(left.counterAdhoc).isTrue()
        val right = matches[1] as OrderSideMatch
        assertThat(right.counterHash).isEqualTo(Word.apply("1855813a4636aa7dc4078f1412c483264e1b0ff45c3b90910b5800669f242c2c"))
        assertThat(right.side).isEqualTo(OrderSide.RIGHT)
        assertThat(right.maker).isEqualTo(Address.apply("0x6a67E811868e54f7d4547F5FeBA0bE8A043f7837"))
        assertThat(right.taker).isEqualTo(Address.apply("0x317a690C61951E0b05A4Bd65FaBf483f98de44cd"))
        assertThat(right.make).isEqualTo(expectedPayment)
        assertThat(right.take).isEqualTo(expectedNft)
        assertThat(right.fill).isEqualTo(expectedNft.value)
        assertThat(right.date).isEqualTo(data)
        assertThat(right.makeUsd).isEqualTo(bidOrderUsd.makeUsd)
        assertThat(right.takeUsd).isEqualTo(bidOrderUsd.takeUsd)
        assertThat(right.makePriceUsd).isEqualTo(bidOrderUsd.makePriceUsd)
        assertThat(right.takePriceUsd).isEqualTo(bidOrderUsd.takePriceUsd)
        assertThat(right.makeValue).isEqualTo(BigDecimal("0.796000000000000000"))
        assertThat(right.takeValue).isEqualTo(BigDecimal.ONE)
        assertThat(right.source).isEqualTo(HistorySource.LOOKSRARE)
        assertThat(left.adhoc).isFalse()
        assertThat(left.counterAdhoc).isTrue()
    }

    @Test
    fun `should convert bid event`() = runBlocking<Unit> {
        val data = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val bidOrderUsd = randomBidOrderUsdValue()
        val sellOrderUsd = randomSellOrderUsdValue()
        val contract = Erc20Token(name = "", id = randomAddress(), symbol = "", decimals = 18)
        val log = log(
            listOf(
                Word.apply("0x9aaa45d6db2ef74ead0751ea9113263d1dec1b50cea05f0ca2002cb8063564a4"),
            ),
            "5b05cb79586cff5aa741ed716514bdd17cb83c374a0093f0e33f6116c3142bbd" +
                    "000000000000000000000000000000000000000000000000000000000000001a" +
                    "0000000000000000000000000000000000000000000000000000000000000001" +
                    "00000000000000000000000041af51792cdcfab9bdc0239f1d1c274e0b2682ae" +
                    "00000000000000000000000077c0c1c3d55a9afad3ad19f231259cf78a203a8d" +
                    "0000000000000000000000000000000000000000000000000000000000000001" +
                    "000000000000000000000000c02aaa39b223fe8d0a0e5c4f27ead9083c756cc2" +
                    "00000000000000000000000049cf6f5d44e70224e2e23fdcdd2c053f30ada28b" +
                    "00000000000000000000000000000000000000000000000000000000000001e0" +
                    "0000000000000000000000000000000000000000000000000000000000000220" +
                    "00000000000000000000000041af51792cdcfab9bdc0239f1d1c274e0b2682ae" +
                    "0000000000000000000000000000000000000000000000000000000000000000" +
                    "0000000000000000000000000000000000000000000000001f34fcbc626bc000" +
                    "0000000000000000000000000000000000000000000000000000000000000000" +
                    "0000000000000000000000000000000000000000000000000028254a45f64000" +
                    "0000000000000000000000000000000000000000000000000000000000000001" +
                    "0000000000000000000000000000000000000000000000000000000000002bd6" +
                    "0000000000000000000000000000000000000000000000000000000000000001" +
                    "0000000000000000000000000000000000000000000000000000000000000001"
        )
        val nftAssetType = Erc721AssetType(
            Address.apply("0x49cf6f5d44e70224e2e23fdcdd2c053f30ada28b"),
            EthUInt256.of("11222")
        )
        val expectedNft = Asset(
            nftAssetType,
            EthUInt256.ONE
        )
        val expectedPayment = Asset(
            Erc20AssetType(Address.apply("0xc02aaa39b223fe8d0a0e5c4f27ead9083c756cc2")),
            EthUInt256.of("2248700000000000000")
        )
        coEvery {
            contractService.get(Address.apply("0xc02aaa39b223fe8d0a0e5c4f27ead9083c756cc2"))
        } returns contract
        coEvery {
            priceUpdateService.getAssetsUsdValue(make = expectedNft, take = expectedPayment, at = data)
        } returns sellOrderUsd

        coEvery {
            priceUpdateService.getAssetsUsdValue(make = expectedPayment, take = expectedNft, at = data)
        } returns bidOrderUsd

        coEvery { tokenStandardProvider.getTokenStandard(nftAssetType.token) } returns TokenStandard.ERC721
        coEvery { orderRepository.findByMakeAndByCounters(any(), any(), any()) } returns emptyFlow()

        val matches = descriptorAsk.convert<OrderSideMatch>(log, data.epochSecond, 1, 0)

        assertThat(matches).hasSize(2)
        val left = matches[0] as OrderSideMatch
        assertThat(left.hash).isEqualTo(Word.apply("0x5b05cb79586cff5aa741ed716514bdd17cb83c374a0093f0e33f6116c3142bbd"))
        assertThat(left.side).isEqualTo(OrderSide.LEFT)
        assertThat(left.maker).isEqualTo(Address.apply("0x41af51792CDcfAB9bDC0239f1D1c274e0B2682Ae"))
        assertThat(left.taker).isEqualTo(Address.apply("0x77C0c1c3d55A9aFad3ad19f231259cf78A203a8D"))
        assertThat(left.make).isEqualTo(expectedPayment)
        assertThat(left.take).isEqualTo(expectedNft)
        assertThat(left.fill).isEqualTo(expectedNft.value)
        assertThat(left.date).isEqualTo(data)
        assertThat(left.makeUsd).isEqualTo(bidOrderUsd.makeUsd)
        assertThat(left.takeUsd).isEqualTo(bidOrderUsd.takeUsd)
        assertThat(left.makePriceUsd).isEqualTo(bidOrderUsd.makePriceUsd)
        assertThat(left.takePriceUsd).isEqualTo(bidOrderUsd.takePriceUsd)
        assertThat(left.makeValue).isEqualTo(BigDecimal("2.248700000000000000"))
        assertThat(left.takeValue).isEqualTo(BigDecimal.ONE)
        assertThat(left.source).isEqualTo(HistorySource.LOOKSRARE)
        assertThat(left.adhoc).isFalse()
        assertThat(left.counterAdhoc).isTrue()
        val right = matches[1] as OrderSideMatch
        assertThat(right.counterHash).isEqualTo(Word.apply("0x5b05cb79586cff5aa741ed716514bdd17cb83c374a0093f0e33f6116c3142bbd"))
        assertThat(right.side).isEqualTo(OrderSide.RIGHT)
        assertThat(right.maker).isEqualTo(Address.apply("0x77C0c1c3d55A9aFad3ad19f231259cf78A203a8D"))
        assertThat(right.taker).isEqualTo(Address.apply("0x41af51792CDcfAB9bDC0239f1D1c274e0B2682Ae"))
        assertThat(right.make).isEqualTo(expectedNft)
        assertThat(right.take).isEqualTo(expectedPayment)
        assertThat(right.fill).isEqualTo(expectedNft.value)
        assertThat(right.date).isEqualTo(data)
        assertThat(right.makeUsd).isEqualTo(sellOrderUsd.makeUsd)
        assertThat(right.takeUsd).isEqualTo(sellOrderUsd.takeUsd)
        assertThat(right.makePriceUsd).isEqualTo(sellOrderUsd.makePriceUsd)
        assertThat(right.takePriceUsd).isEqualTo(sellOrderUsd.takePriceUsd)
        assertThat(right.makeValue).isEqualTo(BigDecimal.ONE)
        assertThat(right.takeValue).isEqualTo(BigDecimal("2.248700000000000000"))
        assertThat(right.source).isEqualTo(HistorySource.LOOKSRARE)
        assertThat(left.adhoc).isFalse()
        assertThat(left.counterAdhoc).isTrue()
    }
}
