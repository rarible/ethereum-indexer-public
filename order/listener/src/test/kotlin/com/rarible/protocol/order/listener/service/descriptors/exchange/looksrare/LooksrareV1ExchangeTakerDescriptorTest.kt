package com.rarible.protocol.order.listener.service.descriptors.exchange.looksrare

import com.rarible.core.contract.model.Erc20Token
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.core.test.data.randomAddress
import com.rarible.ethereum.contract.service.ContractService
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.data.createSellOrder
import com.rarible.protocol.order.core.data.randomBidOrderUsdValue
import com.rarible.protocol.order.core.data.randomSellOrderUsdValue
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderCancel
import com.rarible.protocol.order.core.model.OrderExchangeHistory
import com.rarible.protocol.order.core.model.OrderSide
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.model.TokenStandard
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.ContractsProvider
import com.rarible.protocol.order.core.service.PriceNormalizer
import com.rarible.protocol.order.core.service.PriceUpdateService
import com.rarible.protocol.order.listener.data.log
import com.rarible.protocol.order.core.metric.ForeignOrderMetrics
import com.rarible.protocol.order.listener.misc.convert
import com.rarible.protocol.order.listener.service.looksrare.TokenStandardProvider
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import scalether.domain.Address
import scalether.domain.response.Transaction
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.time.temporal.ChronoUnit

internal class LooksrareV1ExchangeTakerDescriptorTest {

    private val contractsProvider = mockk<ContractsProvider> {
        every { looksrareV1() } returns listOf(randomAddress())
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

    private val descriptorBid = LooksrareV1ExchangeTakerBidDescriptor(
        contractsProvider,
        orderRepository,
        wrapperLooksrareMetric,
        tokenStandardProvider,
        priceUpdateService,
        prizeNormalizer,
        metrics
    )
    private val descriptorAsk = LooksrareV1ExchangeTakerAskDescriptor(
        contractsProvider,
        orderRepository,
        wrapperLooksrareMetric,
        tokenStandardProvider,
        priceUpdateService,
        prizeNormalizer,
        metrics
    )

    @Test
    fun `should convert erc721 sell event`() = runBlocking<Unit> {
        val data = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val bidOrderUsd = randomBidOrderUsdValue()
        val sellOrderUsd = randomSellOrderUsdValue()
        val log = log(
            listOf(
                Word.apply("0x95fb6205e23ff6bda16a2d1dba56b9ad7c783f67c96fa149785052f47696f2be"),
                Word.apply("0x00000000000000000000000047921676a46ccfe3d80b161c7b4ddc8ed9e716b6"),
                Word.apply("0x0000000000000000000000006c8ba1dafb22eae61e9cd3da724cbc3d164c27b9"),
                Word.apply("0x000000000000000000000000732319a3590e4fa838c111826f9584a9a2fdea1a")
            ),
            "0x25765a7f3ffe3496f5118a663a8fbc88fd836d05aaf125e8cdac4fea0e9bd4ce0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000c778417e063141139fce010982780140aa0cd5ab0000000000000000000000000fa6a99c66085b25552930be7961d8928061a84247921676a46ccfe3d80b161c7b4ddc8ed9e716b60000000000000000000000010000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000002386f26fc10000"
        )
        val nftAssetType = Erc721AssetType(
            Address.apply("0x0fa6a99c66085b25552930be7961d8928061a842"),
            EthUInt256.of("32372326957878872325869669322028881416287194712918919938492792330334129618945")
        )
        val expectedNft = Asset(
            nftAssetType,
            EthUInt256.ONE
        )
        val expectedPayment = Asset(
            EthAssetType,
            EthUInt256.of("10000000000000000")
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
        assertThat(left.hash)
            .isEqualTo(Word.apply("0x25765a7f3ffe3496f5118a663a8fbc88fd836d05aaf125e8cdac4fea0e9bd4ce"))
        assertThat(left.side).isEqualTo(OrderSide.LEFT)
        assertThat(left.maker).isEqualTo(Address.apply("0x6c8ba1dafb22eae61e9cd3da724cbc3d164c27b9"))
        assertThat(left.taker).isEqualTo(Address.apply("0x47921676A46CcFe3D80b161c7B4DDC8Ed9e716B6"))
        assertThat(left.make).isEqualTo(expectedNft)
        assertThat(left.take).isEqualTo(expectedPayment)
        assertThat(left.fill).isEqualTo(expectedNft.value)
        assertThat(left.date).isEqualTo(data)
        assertThat(left.makeUsd).isEqualTo(sellOrderUsd.makeUsd)
        assertThat(left.takeUsd).isEqualTo(sellOrderUsd.takeUsd)
        assertThat(left.makePriceUsd).isEqualTo(sellOrderUsd.makePriceUsd)
        assertThat(left.takePriceUsd).isEqualTo(sellOrderUsd.takePriceUsd)
        assertThat(left.takeValue).isEqualTo(BigDecimal("0.010000000000000000"))
        assertThat(left.makeValue).isEqualTo(BigDecimal.ONE)
        assertThat(left.source).isEqualTo(HistorySource.LOOKSRARE)
        assertThat(left.adhoc).isFalse()
        assertThat(left.counterAdhoc).isTrue()
        val right = matches[1] as OrderSideMatch
        assertThat(right.counterHash)
            .isEqualTo(Word.apply("0x25765a7f3ffe3496f5118a663a8fbc88fd836d05aaf125e8cdac4fea0e9bd4ce"))
        assertThat(right.side).isEqualTo(OrderSide.RIGHT)
        assertThat(right.maker).isEqualTo(Address.apply("0x47921676A46CcFe3D80b161c7B4DDC8Ed9e716B6"))
        assertThat(right.taker).isEqualTo(Address.apply("0x6c8ba1dafb22eae61e9cd3da724cbc3d164c27b9"))
        assertThat(right.make).isEqualTo(expectedPayment)
        assertThat(right.take).isEqualTo(expectedNft)
        assertThat(right.fill).isEqualTo(expectedNft.value)
        assertThat(right.date).isEqualTo(data)
        assertThat(right.makeUsd).isEqualTo(bidOrderUsd.makeUsd)
        assertThat(right.takeUsd).isEqualTo(bidOrderUsd.takeUsd)
        assertThat(right.makePriceUsd).isEqualTo(bidOrderUsd.makePriceUsd)
        assertThat(right.takePriceUsd).isEqualTo(bidOrderUsd.takePriceUsd)
        assertThat(right.makeValue).isEqualTo(BigDecimal("0.010000000000000000"))
        assertThat(right.takeValue).isEqualTo(BigDecimal.ONE)
        assertThat(right.source).isEqualTo(HistorySource.LOOKSRARE)
        assertThat(left.adhoc).isFalse()
        assertThat(left.counterAdhoc).isTrue()
    }

    @Test
    fun `should convert erc721 sell through GEM`() = runBlocking<Unit> {
        val transaction = mockk<Transaction> {
            every { input() } returns Binary.apply(
                "0x9a2b81150000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000200000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000023a35e64e734000000000000000000000000000000000000000000000000000000000000000006000000000000000000000000000000000000000000000000000000000000003a4f3e816230000000000000000000000000000000000000000000000000000000000000040000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000011dfe52d06b400000000000000000000000000000000000000000000000000000000000000008160000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000003a00000000000000000000000000000000000000000000000000000000631048b7000000000000000000000000000000000000000000000000000000006337d5ad000000000000000000000000000000000000000000000000000000000000235a000000000000000000000000000000000000000000000000000000000000001b2cbba6756f4b4930e98aa03994f7462487f0de07158640b04ca9260ea2bf21020c8a8133cb7d4fa043abb56de1aefe0779163cdd0c3c210794f017a038125e0d0000000000000000000000009b5ac41ebd9f079cda878f41d4d7ad32c22ac4860000000000000000000000008dcb8b2d721c022552d826f8bcf2995747248d310000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000011c37937e08000000000000000000000000000000000000000000000000000000000000000011a30000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000d0000000000000000000000000000000000000000000000000000000063105397000000000000000000000000000000000000000000000000000000006337e080000000000000000000000000000000000000000000000000000000000000235a000000000000000000000000000000000000000000000000000000000000001ba616498a250a7ae35390ba4fefea2d878b4cacee1d16c19ee90b3dbbab7a6a2e2e26d5d3ecfb6d86ba7fe685a4a54649092d6f1d6f286e44663bfa4fa6f01068000000000000000000000000af9727db19690208db9df2c9ebfd0ef891cd30c10000000000000000000000008dcb8b2d721c022552d826f8bcf2995747248d3100000000000000000000000000000000000000000000000000000000"
            )
            every { from() } returns Address.apply("0x94dbb3db4102181186fd127e6f81564d1d4a20a3")
            every { to() } returns randomAddress()
        }
        val data = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val bidOrderUsd = randomBidOrderUsdValue()
        val sellOrderUsd = randomSellOrderUsdValue()
        val log = log(
            listOf(
                Word.apply("0x95fb6205e23ff6bda16a2d1dba56b9ad7c783f67c96fa149785052f47696f2be"),
                Word.apply("0x00000000000000000000000083c8f28c26bf6aaca652df1dbbe0e1b56f8baba2"),
                Word.apply("0x0000000000000000000000009b5ac41ebd9f079cda878f41d4d7ad32c22ac486"),
                Word.apply("0x00000000000000000000000056244bb70cbd3ea9dc8007399f61dfc065190031")
            ),
            "0x1564ad0588e637597a0f3b3e28ab0afa511557ebcc3ed2de512059749f83c60c000000000000000000000000000000000000000000000000000000000000003a000000000000000000000000c02aaa39b223fe8d0a0e5c4f27ead9083c756cc20000000000000000000000008dcb8b2d721c022552d826f8bcf2995747248d3100000000000000000000000000000000000000000000000000000000000008160000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000011dfe52d06b4000"
        )
        coEvery { contractService.get(any()) } returns Erc20Token(
            id = randomAddress(), name = "test", symbol = "1", decimals = 18
        )
        coEvery { priceUpdateService.getAssetsUsdValue(make = any(), take = any(), at = data) } returns sellOrderUsd
        coEvery { priceUpdateService.getAssetsUsdValue(make = any(), take = any(), at = data) } returns bidOrderUsd
        coEvery { tokenStandardProvider.getTokenStandard(any()) } returns TokenStandard.ERC721
        coEvery { orderRepository.findByMakeAndByCounters(any(), any(), any()) } returns emptyFlow()

        val matches = descriptorBid.convert<OrderSideMatch>(log, transaction, data.epochSecond, 0, 1)

        assertThat(matches).hasSize(2)
        val left = matches[0] as OrderSideMatch
        assertThat(left.taker).isEqualTo(Address.apply("0x94dbb3db4102181186fd127e6f81564d1d4a20a3"))
        val right = matches[1] as OrderSideMatch
        assertThat(right.maker).isEqualTo(Address.apply("0x94dbb3db4102181186fd127e6f81564d1d4a20a3"))
    }

    @Test
    fun `should convert erc721 bid event`() = runBlocking<Unit> {
        val transaction = mockk<Transaction> { every { input() } returns Binary.empty() }
        val date = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val contract = Erc20Token(name = "", id = randomAddress(), symbol = "", decimals = 18)
        val bidOrderUsd = randomBidOrderUsdValue()
        val sellOrderUsd = randomSellOrderUsdValue()
        val log = log(
            listOf(
                Word.apply("0x68cd251d4d267c6e2034ff0088b990352b97b2002c0476587d0c4da889c11330"),
                Word.apply("0x00000000000000000000000047921676a46ccfe3d80b161c7b4ddc8ed9e716b6"),
                Word.apply("0x0000000000000000000000006c8ba1dafb22eae61e9cd3da724cbc3d164c27b9"),
                Word.apply("0x000000000000000000000000732319a3590e4fa838c111826f9584a9a2fdea1a"),
            ),
            "a04ce3cc6721c3a7882a76705f77b4ce17a006b7cec01ac5a033f36f081847720000000000000000000000000000000000000000000000000000000000000001000000000000000000000000c778417e063141139fce010982780140aa0cd5ab0000000000000000000000000fa6a99c66085b25552930be7961d8928061a84247921676a46ccfe3d80b161c7b4ddc8ed9e716b6000000000000000000000001000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000470de4df820000"
        )
        val nftAssetType = Erc721AssetType(
            Address.apply("0x0fa6a99c66085b25552930be7961d8928061a842"),
            EthUInt256.of("32372326957878872325869669322028881416287194712918919938492792330334129618945")
        )
        val expectedNft = Asset(
            nftAssetType,
            EthUInt256.ONE
        )
        val expectedPayment = Asset(
            Erc20AssetType(Address.apply("0xc778417e063141139fce010982780140aa0cd5ab")),
            EthUInt256.of("20000000000000000")
        )
        coEvery { contractService.get(Address.apply("0xc778417e063141139fce010982780140aa0cd5ab")) } returns contract
        coEvery {
            priceUpdateService.getAssetsUsdValue(make = expectedNft, take = expectedPayment, at = date)
        } returns sellOrderUsd

        coEvery {
            priceUpdateService.getAssetsUsdValue(make = expectedPayment, take = expectedNft, at = date)
        } returns bidOrderUsd

        coEvery { tokenStandardProvider.getTokenStandard(nftAssetType.token) } returns TokenStandard.ERC721
        coEvery { orderRepository.findByMakeAndByCounters(any(), any(), any()) } returns emptyFlow()

        val matches = descriptorAsk.convert<OrderSideMatch>(log, date.epochSecond, 0, 1)

        assertThat(matches).hasSize(2)
        val left = matches[0] as OrderSideMatch
        assertThat(left.hash).isEqualTo(Word.apply("0xa04ce3cc6721c3a7882a76705f77b4ce17a006b7cec01ac5a033f36f08184772"))
        assertThat(left.side).isEqualTo(OrderSide.LEFT)
        assertThat(left.maker).isEqualTo(Address.apply("0x6c8ba1dafb22eae61e9cd3da724cbc3d164c27b9"))
        assertThat(left.taker).isEqualTo(Address.apply("0x47921676A46CcFe3D80b161c7B4DDC8Ed9e716B6"))
        assertThat(left.make).isEqualTo(expectedPayment)
        assertThat(left.take).isEqualTo(expectedNft)
        assertThat(left.fill).isEqualTo(expectedNft.value)
        assertThat(left.date).isEqualTo(date)
        assertThat(left.makeUsd).isEqualTo(bidOrderUsd.makeUsd)
        assertThat(left.takeUsd).isEqualTo(bidOrderUsd.takeUsd)
        assertThat(left.makePriceUsd).isEqualTo(bidOrderUsd.makePriceUsd)
        assertThat(left.takePriceUsd).isEqualTo(bidOrderUsd.takePriceUsd)
        assertThat(left.makeValue).isEqualTo(BigDecimal("0.020000000000000000"))
        assertThat(left.takeValue).isEqualTo(BigDecimal.ONE)
        assertThat(left.source).isEqualTo(HistorySource.LOOKSRARE)
        assertThat(left.adhoc).isFalse()
        assertThat(left.counterAdhoc).isTrue()
        val right = matches[1] as OrderSideMatch
        assertThat(right.counterHash)
            .isEqualTo(Word.apply("0xa04ce3cc6721c3a7882a76705f77b4ce17a006b7cec01ac5a033f36f08184772"))
        assertThat(right.side).isEqualTo(OrderSide.RIGHT)
        assertThat(right.maker).isEqualTo(Address.apply("0x47921676A46CcFe3D80b161c7B4DDC8Ed9e716B6"))
        assertThat(right.taker).isEqualTo(Address.apply("0x6c8ba1dafb22eae61e9cd3da724cbc3d164c27b9"))
        assertThat(right.make).isEqualTo(expectedNft)
        assertThat(right.take).isEqualTo(expectedPayment)
        assertThat(right.fill).isEqualTo(expectedNft.value)
        assertThat(right.date).isEqualTo(date)
        assertThat(right.makeUsd).isEqualTo(sellOrderUsd.makeUsd)
        assertThat(right.takeUsd).isEqualTo(sellOrderUsd.takeUsd)
        assertThat(right.makePriceUsd).isEqualTo(sellOrderUsd.makePriceUsd)
        assertThat(right.takePriceUsd).isEqualTo(sellOrderUsd.takePriceUsd)
        assertThat(right.takeValue).isEqualTo(BigDecimal("0.020000000000000000"))
        assertThat(right.makeValue).isEqualTo(BigDecimal.ONE)
        assertThat(right.source).isEqualTo(HistorySource.LOOKSRARE)
        assertThat(left.adhoc).isFalse()
        assertThat(left.counterAdhoc).isTrue()
    }

    @Test
    fun `should cancel orders with same nonce`() = runBlocking<Unit> {
        val transaction = mockk<Transaction> { every { input() } returns Binary.empty() }
        val date = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val contract = Erc20Token(name = "", id = randomAddress(), symbol = "", decimals = 18)
        val bidOrderUsd = randomBidOrderUsdValue()
        val sellOrderUsd = randomSellOrderUsdValue()
        val maker = Address.apply("0x6c8ba1dafb22eae61e9cd3da724cbc3d164c27b9")
        val hash = Word.apply("0xa04ce3cc6721c3a7882a76705f77b4ce17a006b7cec01ac5a033f36f08184772")

        val previousOrder = createSellOrder()
        val currentOrder = createSellOrder().copy(id = Order.Id(hash), hash = hash)

        val log = log(
            listOf(
                Word.apply("0x68cd251d4d267c6e2034ff0088b990352b97b2002c0476587d0c4da889c11330"),
                Word.apply("0x00000000000000000000000047921676a46ccfe3d80b161c7b4ddc8ed9e716b6"),
                Word.apply("0x0000000000000000000000006c8ba1dafb22eae61e9cd3da724cbc3d164c27b9"),
                Word.apply("0x000000000000000000000000732319a3590e4fa838c111826f9584a9a2fdea1a"),
            ),
            "a04ce3cc6721c3a7882a76705f77b4ce17a006b7cec01ac5a033f36f081847720000000000000000000000000000000000000000000000000000000000000001000000000000000000000000c778417e063141139fce010982780140aa0cd5ab0000000000000000000000000fa6a99c66085b25552930be7961d8928061a84247921676a46ccfe3d80b161c7b4ddc8ed9e716b6000000000000000000000001000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000470de4df820000"
        )
        val nftAssetType = Erc721AssetType(
            Address.apply("0x0fa6a99c66085b25552930be7961d8928061a842"),
            EthUInt256.of("32372326957878872325869669322028881416287194712918919938492792330334129618945")
        )
        val expectedNft = Asset(nftAssetType, EthUInt256.ONE)

        coEvery { contractService.get(Address.apply("0xc778417e063141139fce010982780140aa0cd5ab")) } returns contract

        coEvery {
            priceUpdateService.getAssetsUsdValue(make = eq(expectedNft), take = any(), at = any())
        } returns sellOrderUsd

        coEvery {
            priceUpdateService.getAssetsUsdValue(make = any(), take = expectedNft, at = any())
        } returns bidOrderUsd

        coEvery { tokenStandardProvider.getTokenStandard(nftAssetType.token) } returns TokenStandard.ERC721
        coEvery {
            orderRepository.findByMakeAndByCounters(Platform.LOOKSRARE, maker, listOf(BigInteger.ONE))
        } returns flowOf(previousOrder, currentOrder)

        val events = descriptorAsk.convert<OrderExchangeHistory>(log, date.epochSecond, 0, 1)

        // 2 side matches, 1 cancel (for current order cancel event should not be emitted)
        assertThat(events).hasSize(3)

        val cancel = events.filterIsInstance<OrderCancel>().first()

        assertThat(cancel.hash).isEqualTo(previousOrder.hash)
    }
}
