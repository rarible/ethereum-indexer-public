package com.rarible.protocol.order.listener.service.opensea

import com.rarible.core.contract.model.Erc20Token
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.core.test.data.randomAddress
import com.rarible.ethereum.contract.service.ContractService
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.exchange.seaport.v1.OrderCancelledEvent
import com.rarible.protocol.contracts.seaport.v1.events.OrderFulfilledEvent
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.data.randomBidOrderUsdValue
import com.rarible.protocol.order.core.data.randomSellOrderUsdValue
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Erc1155AssetType
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.OrderSide
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.core.service.PriceNormalizer
import com.rarible.protocol.order.core.service.PriceUpdateService
import com.rarible.protocol.order.core.trace.TraceCallService
import com.rarible.protocol.order.listener.data.log
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import io.daonomic.rpc.domain.WordFactory
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import scalether.domain.Address
import scalether.domain.response.Transaction
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant

internal class SeaportEventConverterTest {
    private val priceUpdateService = mockk<PriceUpdateService>()
    private val contractService = mockk<ContractService>()
    private val prizeNormalizer = PriceNormalizer(contractService)
    private val traceCallService = mockk<TraceCallService>()
    private val featureFlags = OrderIndexerProperties.FeatureFlags()
    private val wrapperLooksrareMetric = mockk<RegisteredCounter> { every { increment() } returns Unit }

    private val converter = SeaportEventConverter(
        priceUpdateService,
        prizeNormalizer,
        traceCallService,
        featureFlags,
        wrapperLooksrareMetric
    )

    @Test
    fun `should convert basic erc721 sell OrderFulfilledEvent`() = runBlocking<Unit> {
        val data = Instant.now()
        val bidOrderUsd = randomBidOrderUsdValue()
        val sellOrderUsd = randomSellOrderUsdValue()
        val log = log(
            listOf(
                Word.apply("0x9d9af8e38d66c62e2c12f0225249fd9d721c54b83f48d9352c97c6cacdcb6f31"),
                Word.apply("0x0000000000000000000000006c8ba1dafb22eae61e9cd3da724cbc3d164c27b9"),
                Word.apply("0x00000000000000000000000000000000e88fe2628ebc5da81d2b3cead633e89e")
            ),
            "0xb87eea32e0dc18b180b6f8cdf7af6eed7a8c4da45b2c005115b267fd40d86ba900000000000000000000000047921676a46ccfe3d80b161c7b4ddc8ed9e716b600000000000000000000000000000000000000000000000000000000000000800000000000000000000000000000000000000000000000000000000000000120000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000020000000000000000000000000fa6a99c66085b25552930be7961d8928061a84247921676a46ccfe3d80b161c7b4ddc8ed9e716b60000000000000000000000010000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000200000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000006c3f2aac800c0000000000000000000000000006c8ba1dafb22eae61e9cd3da724cbc3d164c27b9000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000002c68af0bb140000000000000000000000000008de9c5a032463c561423387a9648c5c7bcc5bc90"
        )
        val expectedNft = Asset(
            Erc721AssetType(
                Address.apply("0x0fa6a99c66085b25552930be7961d8928061a842"),
                EthUInt256.of("32372326957878872325869669322028881416287194712918919938492792330334129618945")
            ),
            EthUInt256.ONE
        )
        val expectedPayment = Asset(
            EthAssetType,
            EthUInt256.of("500000000000000000")
        )
        coEvery { priceUpdateService.getAssetsUsdValue(make = expectedNft, take = expectedPayment, at = data) } returns sellOrderUsd
        coEvery { priceUpdateService.getAssetsUsdValue(make = expectedPayment, take = expectedNft, at = data) } returns bidOrderUsd

        val event = OrderFulfilledEvent.apply(log)
        val matches = converter.convert(event, data, WordFactory.create())

        assertThat(matches).hasSize(2)
        val left = matches[0]
        assertThat(left.hash).isEqualTo(Word.apply("0xb87eea32e0dc18b180b6f8cdf7af6eed7a8c4da45b2c005115b267fd40d86ba9"))
        assertThat(left.side).isEqualTo(OrderSide.LEFT)
        assertThat(left.maker).isEqualTo(Address.apply("0x6c8ba1dafb22eae61e9cd3da724cbc3d164c27b9"))
        assertThat(left.taker).isEqualTo(Address.apply("0x47921676A46CcFe3D80b161c7B4DDC8Ed9e716B6"))
        assertThat(left.make).isEqualTo(expectedNft)
        assertThat(left.take).isEqualTo(expectedPayment)
        assertThat(left.fill).isEqualTo(expectedPayment.value)
        assertThat(left.date).isEqualTo(data)
        assertThat(left.makeUsd).isEqualTo(sellOrderUsd.makeUsd)
        assertThat(left.takeUsd).isEqualTo(sellOrderUsd.takeUsd)
        assertThat(left.makePriceUsd).isEqualTo(sellOrderUsd.makePriceUsd)
        assertThat(left.takePriceUsd).isEqualTo(sellOrderUsd.takePriceUsd)
        assertThat(left.takeValue).isEqualTo(BigDecimal("0.500000000000000000"))
        assertThat(left.makeValue).isEqualTo(BigDecimal.ONE)
        assertThat(left.source).isEqualTo(HistorySource.OPEN_SEA)
        assertThat(left.adhoc).isFalse()
        assertThat(left.counterAdhoc).isTrue()
        val right = matches[1]
        assertThat(right.counterHash).isEqualTo(Word.apply("0xb87eea32e0dc18b180b6f8cdf7af6eed7a8c4da45b2c005115b267fd40d86ba9"))
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
        assertThat(right.makeValue).isEqualTo(BigDecimal("0.500000000000000000"))
        assertThat(right.takeValue).isEqualTo(BigDecimal.ONE)
        assertThat(right.source).isEqualTo(HistorySource.OPEN_SEA)
        assertThat(left.adhoc).isFalse()
        assertThat(left.counterAdhoc).isTrue()
    }

    @Test
    fun `should convert basic erc721 bid OrderFulfilledEvent`() = runBlocking<Unit> {
        val data = Instant.now()
        val contract = Erc20Token(name = "", id = randomAddress(), symbol = "", decimals = 18)
        val bidOrderUsd = randomBidOrderUsdValue()
        val sellOrderUsd = randomSellOrderUsdValue()
        val log = log(
            listOf(
                Word.apply("0x9d9af8e38d66c62e2c12f0225249fd9d721c54b83f48d9352c97c6cacdcb6f31"),
                Word.apply("0x0000000000000000000000006c8ba1dafb22eae61e9cd3da724cbc3d164c27b9"),
                Word.apply("0x00000000000000000000000000000000e88fe2628ebc5da81d2b3cead633e89e")
            ),
            "0x19a1a135c643899e688647411a33b7a54d02d27a9e85523c0072a5f5752524c000000000000000000000000047921676a46ccfe3d80b161c7b4ddc8ed9e716b60000000000000000000000000000000000000000000000000000000000000080000000000000000000000000000000000000000000000000000000000000012000000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000001000000000000000000000000c778417e063141139fce010982780140aa0cd5ab000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001aa535d3d0c0000000000000000000000000000000000000000000000000000000000000000000200000000000000000000000000000000000000000000000000000000000000020000000000000000000000000fa6a99c66085b25552930be7961d8928061a84247921676a46ccfe3d80b161c7b4ddc8ed9e716b600000000000000000000000100000000000000000000000000000000000000000000000000000000000000010000000000000000000000006c8ba1dafb22eae61e9cd3da724cbc3d164c27b90000000000000000000000000000000000000000000000000000000000000001000000000000000000000000c778417e063141139fce010982780140aa0cd5ab0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000aa87bee5380000000000000000000000000008de9c5a032463c561423387a9648c5c7bcc5bc90"
        )
        val expectedNft = Asset(
            Erc721AssetType(
                Address.apply("0x0fa6a99c66085b25552930be7961d8928061a842"),
                EthUInt256.of("32372326957878872325869669322028881416287194712918919938492792330334129618945")
            ),
            EthUInt256.ONE
        )
        val expectedPayment = Asset(
            Erc20AssetType(Address.apply("0xc778417e063141139fce010982780140aa0cd5ab")),
            EthUInt256.of("120000000000000000")
        )
        coEvery { contractService.get(Address.apply("0xc778417e063141139fce010982780140aa0cd5ab")) } returns contract
        coEvery { priceUpdateService.getAssetsUsdValue(make = expectedNft, take = expectedPayment, at = data) } returns sellOrderUsd
        coEvery { priceUpdateService.getAssetsUsdValue(make = expectedPayment, take = expectedNft, at = data) } returns bidOrderUsd

        val event = OrderFulfilledEvent.apply(log)
        val marker = Word.apply(WordFactory.create().slice(0, 24).add(OrderSideMatch.MARKER_BYTES))
        val matches = converter.convert(event, data, marker)

        assertThat(matches).hasSize(2)
        val left = matches[0]
        assertThat(left.hash).isEqualTo(Word.apply("0x19a1a135c643899e688647411a33b7a54d02d27a9e85523c0072a5f5752524c0"))
        assertThat(left.side).isEqualTo(OrderSide.LEFT)
        assertThat(left.maker).isEqualTo(Address.apply("0x6c8ba1dafb22eae61e9cd3da724cbc3d164c27b9"))
        assertThat(left.taker).isEqualTo(Address.apply("0x47921676A46CcFe3D80b161c7B4DDC8Ed9e716B6"))
        assertThat(left.make).isEqualTo(expectedPayment)
        assertThat(left.take).isEqualTo(expectedNft)
        assertThat(left.fill).isEqualTo(expectedNft.value)
        assertThat(left.date).isEqualTo(data)
        assertThat(left.makeUsd).isEqualTo(bidOrderUsd.makeUsd)
        assertThat(left.takeUsd).isEqualTo(bidOrderUsd.takeUsd)
        assertThat(left.makePriceUsd).isEqualTo(bidOrderUsd.makePriceUsd)
        assertThat(left.takePriceUsd).isEqualTo(bidOrderUsd.takePriceUsd)
        assertThat(left.makeValue).isEqualTo(BigDecimal("0.120000000000000000"))
        assertThat(left.takeValue).isEqualTo(BigDecimal.ONE)
        assertThat(left.source).isEqualTo(HistorySource.OPEN_SEA)
        assertThat(left.adhoc).isFalse()
        assertThat(left.counterAdhoc).isTrue()
        assertThat(left.marketplaceMarker).isNull()
        val right = matches[1]
        assertThat(right.counterHash).isEqualTo(Word.apply("0x19a1a135c643899e688647411a33b7a54d02d27a9e85523c0072a5f5752524c0"))
        assertThat(right.side).isEqualTo(OrderSide.RIGHT)
        assertThat(right.maker).isEqualTo(Address.apply("0x47921676A46CcFe3D80b161c7B4DDC8Ed9e716B6"))
        assertThat(right.taker).isEqualTo(Address.apply("0x6c8ba1dafb22eae61e9cd3da724cbc3d164c27b9"))
        assertThat(right.make).isEqualTo(expectedNft)
        assertThat(right.take).isEqualTo(expectedPayment)
        assertThat(right.fill).isEqualTo(expectedPayment.value)
        assertThat(right.date).isEqualTo(data)
        assertThat(right.makeUsd).isEqualTo(sellOrderUsd.makeUsd)
        assertThat(right.takeUsd).isEqualTo(sellOrderUsd.takeUsd)
        assertThat(right.makePriceUsd).isEqualTo(sellOrderUsd.makePriceUsd)
        assertThat(right.takePriceUsd).isEqualTo(sellOrderUsd.takePriceUsd)
        assertThat(right.takeValue).isEqualTo(BigDecimal("0.120000000000000000"))
        assertThat(right.makeValue).isEqualTo(BigDecimal.ONE)
        assertThat(right.source).isEqualTo(HistorySource.OPEN_SEA)
        assertThat(right.adhoc).isTrue()
        assertThat(right.counterAdhoc).isFalse()
        assertThat(right.marketplaceMarker).isEqualTo(marker)
    }

    @Test
    fun `should convert basic erc1155 full sell OrderFulfilledEvent`() = runBlocking<Unit> {
        val data = Instant.now()
        val log = log(
            listOf(
                Word.apply("0x9d9af8e38d66c62e2c12f0225249fd9d721c54b83f48d9352c97c6cacdcb6f31"),
                Word.apply("0x0000000000000000000000006c8ba1dafb22eae61e9cd3da724cbc3d164c27b9"),
                Word.apply("0x00000000000000000000000000000000e88fe2628ebc5da81d2b3cead633e89e")
            ),
            "0x4d0ae0fee315be795d0fb0f750fe065c99865a52a0fb0ff1a7463961d56e372f00000000000000000000000047921676a46ccfe3d80b161c7b4ddc8ed9e716b600000000000000000000000000000000000000000000000000000000000000800000000000000000000000000000000000000000000000000000000000000120000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000030000000000000000000000002ebecabbbe8a8c629b99ab23ed154d74cd5d4342000000000000000000000000000000000000000000000000000000000001aea9000000000000000000000000000000000000000000000000000000000000000300000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000c2d8199ce67c0000000000000000000000000006c8ba1dafb22eae61e9cd3da724cbc3d164c27b9000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000004fefa17b7240000000000000000000000000008de9c5a032463c561423387a9648c5c7bcc5bc90"
        )
        val expectedNft = Asset(
            Erc1155AssetType(
                Address.apply("0x2ebecabbbe8a8c629b99ab23ed154d74cd5d4342"),
                EthUInt256.of("110249")
            ),
            EthUInt256.of(3)
        )
        val expectedPayment = Asset(
            EthAssetType,
            EthUInt256.of("900000000000000000")
        )
        coEvery { priceUpdateService.getAssetsUsdValue(make = expectedNft, take = expectedPayment, at = data) } returns null
        coEvery { priceUpdateService.getAssetsUsdValue(make = expectedPayment, take = expectedNft, at = data) } returns null

        val event = OrderFulfilledEvent.apply(log)
        val matches = converter.convert(event, data, WordFactory.create())

        val left = matches[0]
        assertThat(left.side).isEqualTo(OrderSide.LEFT)
        assertThat(left.make).isEqualTo(expectedNft)
        assertThat(left.take).isEqualTo(expectedPayment)
        assertThat(left.fill).isEqualTo(expectedPayment.value)
        assertThat(left.takeValue).isEqualTo(BigDecimal("0.900000000000000000"))
        assertThat(left.makeValue).isEqualTo(BigDecimal.valueOf(3))
        val right = matches[1]
        assertThat(right.side).isEqualTo(OrderSide.RIGHT)
        assertThat(right.make).isEqualTo(expectedPayment)
        assertThat(right.take).isEqualTo(expectedNft)
        assertThat(right.fill).isEqualTo(expectedNft.value)
        assertThat(right.makeValue).isEqualTo(BigDecimal("0.900000000000000000"))
        assertThat(right.takeValue).isEqualTo(BigDecimal.valueOf(3))
    }

    @Test
    fun `should convert basic erc1155 full bid OrderFulfilledEvent`() = runBlocking<Unit> {
        val data = Instant.now()
        val contract = Erc20Token(name = "", id = randomAddress(), symbol = "", decimals = 18)
        val log = log(
            listOf(
                Word.apply("0x9d9af8e38d66c62e2c12f0225249fd9d721c54b83f48d9352c97c6cacdcb6f31"),
                Word.apply("0x0000000000000000000000006c8ba1dafb22eae61e9cd3da724cbc3d164c27b9"),
                Word.apply("0x00000000000000000000000000000000e88fe2628ebc5da81d2b3cead633e89e")
            ),
            "0xfd4cd594d6e06ab46a987c95422e9515a4da65a583671b288009b0216395d1e100000000000000000000000047921676a46ccfe3d80b161c7b4ddc8ed9e716b60000000000000000000000000000000000000000000000000000000000000080000000000000000000000000000000000000000000000000000000000000012000000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000001000000000000000000000000c778417e063141139fce010982780140aa0cd5ab0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000008e1bc9bf040000000000000000000000000000000000000000000000000000000000000000000200000000000000000000000000000000000000000000000000000000000000030000000000000000000000002ebecabbbe8a8c629b99ab23ed154d74cd5d4342000000000000000000000000000000000000000000000000000000000001aea900000000000000000000000000000000000000000000000000000000000000020000000000000000000000006c8ba1dafb22eae61e9cd3da724cbc3d164c27b90000000000000000000000000000000000000000000000000000000000000001000000000000000000000000c778417e063141139fce010982780140aa0cd5ab000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000038d7ea4c680000000000000000000000000008de9c5a032463c561423387a9648c5c7bcc5bc90"
        )
        val expectedNft = Asset(
            Erc1155AssetType(
                Address.apply("0x2ebecabbbe8a8c629b99ab23ed154d74cd5d4342"),
                EthUInt256.of("110249")
            ),
            EthUInt256.of(2)
        )
        val expectedPayment = Asset(
            Erc20AssetType(Address.apply("0xc778417e063141139fce010982780140aa0cd5ab")),
            EthUInt256.of("40000000000000000")
        )
        coEvery { contractService.get(Address.apply("0xc778417e063141139fce010982780140aa0cd5ab")) } returns contract
        coEvery { priceUpdateService.getAssetsUsdValue(make = expectedNft, take = expectedPayment, at = data) } returns null
        coEvery { priceUpdateService.getAssetsUsdValue(make = expectedPayment, take = expectedNft, at = data) } returns null

        val event = OrderFulfilledEvent.apply(log)
        val matches = converter.convert(event, data, WordFactory.create())

        val left = matches[0]
        assertThat(left.side).isEqualTo(OrderSide.LEFT)
        assertThat(left.make).isEqualTo(expectedPayment)
        assertThat(left.take).isEqualTo(expectedNft)
        assertThat(left.fill).isEqualTo(expectedNft.value)
        assertThat(left.makeValue).isEqualTo(BigDecimal("0.040000000000000000"))
        assertThat(left.takeValue).isEqualTo(BigDecimal.valueOf(2))
        val right = matches[1]
        assertThat(right.side).isEqualTo(OrderSide.RIGHT)
        assertThat(right.make).isEqualTo(expectedNft)
        assertThat(right.take).isEqualTo(expectedPayment)
        assertThat(right.fill).isEqualTo(expectedPayment.value)
        assertThat(right.takeValue).isEqualTo(BigDecimal("0.040000000000000000"))
        assertThat(right.makeValue).isEqualTo(BigDecimal.valueOf(2))
    }

    @Test
    fun `should convert basic erc1155 partly sell OrderFulfilledEvent`() = runBlocking<Unit> {
        val data = Instant.now()
        val log = log(
            listOf(
                Word.apply("0x9d9af8e38d66c62e2c12f0225249fd9d721c54b83f48d9352c97c6cacdcb6f31"),
                Word.apply("0x0000000000000000000000006c8ba1dafb22eae61e9cd3da724cbc3d164c27b9"),
                Word.apply("0x00000000000000000000000000000000e88fe2628ebc5da81d2b3cead633e89e")
            ),
            "0x5ec2493878f9575f3ae7568b94b3a021dd830e46e5b0005d8e23a825dfb7323a00000000000000000000000047921676a46ccfe3d80b161c7b4ddc8ed9e716b600000000000000000000000000000000000000000000000000000000000000800000000000000000000000000000000000000000000000000000000000000120000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000030000000000000000000000002ebecabbbe8a8c629b99ab23ed154d74cd5d4342000000000000000000000000000000000000000000000000000000000001aea9000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000067eab853ae20000000000000000000000000006c8ba1dafb22eae61e9cd3da724cbc3d164c27b90000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000002aa1efb94e0000000000000000000000000008de9c5a032463c561423387a9648c5c7bcc5bc90"
        )
        val expectedNft = Asset(
            Erc1155AssetType(
                Address.apply("0x2ebecabbbe8a8c629b99ab23ed154d74cd5d4342"),
                EthUInt256.of("110249")
            ),
            EthUInt256.of(1)
        )
        val expectedPayment = Asset(
            EthAssetType,
            EthUInt256.of("30000000000000000")
        )
        coEvery { priceUpdateService.getAssetsUsdValue(make = expectedNft, take = expectedPayment, at = data) } returns null
        coEvery { priceUpdateService.getAssetsUsdValue(make = expectedPayment, take = expectedNft, at = data) } returns null

        val event = OrderFulfilledEvent.apply(log)
        val matches = converter.convert(event, data, WordFactory.create())

        val left = matches[0]
        assertThat(left.hash).isEqualTo(Word.apply("0x5ec2493878f9575f3ae7568b94b3a021dd830e46e5b0005d8e23a825dfb7323a"))
        assertThat(left.side).isEqualTo(OrderSide.LEFT)
        assertThat(left.make).isEqualTo(expectedNft)
        assertThat(left.take).isEqualTo(expectedPayment)
        assertThat(left.fill).isEqualTo(expectedPayment.value)
        assertThat(left.takeValue).isEqualTo(BigDecimal("0.030000000000000000"))
        assertThat(left.makeValue).isEqualTo(BigDecimal.valueOf(1))
        val right = matches[1]
        assertThat(right.side).isEqualTo(OrderSide.RIGHT)
        assertThat(right.make).isEqualTo(expectedPayment)
        assertThat(right.take).isEqualTo(expectedNft)
        assertThat(right.fill).isEqualTo(expectedNft.value)
        assertThat(right.makeValue).isEqualTo(BigDecimal("0.030000000000000000"))
        assertThat(right.takeValue).isEqualTo(BigDecimal.valueOf(1))
    }

    @Test
    fun `should convert basic erc721 sell OrderCancelledEvent`() = runBlocking<Unit> {
        val data = Instant.now()
        val log = log(
            listOf(
                Word.apply("0x6bacc01dbe442496068f7d234edd811f1a5f833243e0aec824f86ab861f3c90d"),
                Word.apply("0x00000000000000000000000047921676a46ccfe3d80b161c7b4ddc8ed9e716b6"),
                Word.apply("0x00000000000000000000000000000000e88fe2628ebc5da81d2b3cead633e89e")
            ),
            "0xd50e7950c22b29d6ea97ed07617c14d4b73a7614d3ad2f40779ef0b700535026"
        )
        val transaction = transaction(
            "0xfd9f1e1000000000000000000000000000000000000000000000000000000000000000200000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000002000000000000000000000000047921676a46ccfe3d80b161c7b4ddc8ed9e716b600000000000000000000000000000000e88fe2628ebc5da81d2b3cead633e89e0000000000000000000000000000000000000000000000000000000000000160000000000000000000000000000000000000000000000000000000000000022000000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000062baaae10000000000000000000000000000000000000000000000000000000062e237e1000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000ca1062fc5dd76c0000007b02230091a7ed01230072f7006a004d60a8d4e71d599b8104250f00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000020000000000000000000000006ede7f3c26975aad32a475e1021d8f6f39c89d8247921676a46ccfe3d80b161c7b4ddc8ed9e716b600000000000000000000005b000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000002000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000019faae14eb88000000000000000000000000000000000000000000000000000019faae14eb8800000000000000000000000000047921676a46ccfe3d80b161c7b4ddc8ed9e716b6000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000aa87bee538000000000000000000000000000000000000000000000000000000aa87bee5380000000000000000000000000008de9c5a032463c561423387a9648c5c7bcc5bc90"
        )
        val expectedNft = Asset(
            Erc721AssetType(
                Address.apply("0x6ede7f3c26975aad32a475e1021d8f6f39c89d82"),
                EthUInt256.of("32372326957878872325869669322028881416287194712918919938492792330334129619035")
            ),
            EthUInt256.ONE
        )
        val expectedPayment = Asset(
            EthAssetType,
            EthUInt256.of("120000000000000000")
        )
        val event = OrderCancelledEvent.apply(log)
        val cancels = converter.convert(event, transaction, 0, 1, data)

        assertThat(cancels).hasSize(1)
        val cancel = cancels.single()
        assertThat(cancel.date).isEqualTo(data)
        assertThat(cancel.hash).isEqualTo(Word.apply("0xd50e7950c22b29d6ea97ed07617c14d4b73a7614d3ad2f40779ef0b700535026"))
        assertThat(cancel.maker).isEqualTo(Address.apply("0x47921676a46ccfe3d80b161c7b4ddc8ed9e716b6"))
        assertThat(cancel.make).isEqualTo(expectedNft)
        assertThat(cancel.take).isEqualTo(expectedPayment)
    }

    @Test
    fun `should convert basic erc721 bid OrderCancelledEvent`() = runBlocking<Unit> {
        val data = Instant.now()
        val log = log(
            listOf(
                Word.apply("0x6bacc01dbe442496068f7d234edd811f1a5f833243e0aec824f86ab861f3c90d"),
                Word.apply("0x0000000000000000000000006c8ba1dafb22eae61e9cd3da724cbc3d164c27b9"),
                Word.apply("0x00000000000000000000000000000000e88fe2628ebc5da81d2b3cead633e89e")
            ),
            "0x76cefc5152c4b9a846f2a85156e749e7d41967c4568694d7b54ceb8191607e48"
        )
        val transaction = transaction(
            "0xfd9f1e100000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000200000000000000000000000006c8ba1dafb22eae61e9cd3da724cbc3d164c27b900000000000000000000000000000000e88fe2628ebc5da81d2b3cead633e89e0000000000000000000000000000000000000000000000000000000000000160000000000000000000000000000000000000000000000000000000000000022000000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000062baaf400000000000000000000000000000000000000000000000000000000062bea39c000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000e91bdfab2420580000007b02230091a7ed01230072f7006a004d60a8d4e71d599b8104250f0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000001000000000000000000000000c778417e063141139fce010982780140aa0cd5ab000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000002830a8a8058800000000000000000000000000000000000000000000000000002830a8a80588000000000000000000000000000000000000000000000000000000000000000000200000000000000000000000000000000000000000000000000000000000000020000000000000000000000006ede7f3c26975aad32a475e1021d8f6f39c89d8247921676a46ccfe3d80b161c7b4ddc8ed9e716b600000000000000000000005b000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000010000000000000000000000006c8ba1dafb22eae61e9cd3da724cbc3d164c27b90000000000000000000000000000000000000000000000000000000000000001000000000000000000000000c778417e063141139fce010982780140aa0cd5ab000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000101376a99bd00000000000000000000000000000000000000000000000000000101376a99bd0000000000000000000000000008de9c5a032463c561423387a9648c5c7bcc5bc90"
        )
        val expectedNft = Asset(
            Erc721AssetType(
                Address.apply("0x6ede7f3c26975aad32a475e1021d8f6f39c89d82"),
                EthUInt256.of("32372326957878872325869669322028881416287194712918919938492792330334129619035")
            ),
            EthUInt256.ONE
        )
        val expectedPayment = Asset(
            Erc20AssetType(Address.apply("0xc778417e063141139fce010982780140aa0cd5ab")),
            EthUInt256.of("181000000000000000")
        )
        val event = OrderCancelledEvent.apply(log)
        val cancels = converter.convert(event, transaction, 0, 1, data)

        assertThat(cancels).hasSize(1)
        val cancel = cancels.single()
        assertThat(cancel.date).isEqualTo(data)
        assertThat(cancel.hash).isEqualTo(Word.apply("0x76cefc5152c4b9a846f2a85156e749e7d41967c4568694d7b54ceb8191607e48"))
        assertThat(cancel.maker).isEqualTo(Address.apply("0x6c8ba1dafb22eae61e9cd3da724cbc3d164c27b9"))
        assertThat(cancel.make).isEqualTo(expectedPayment)
        assertThat(cancel.take).isEqualTo(expectedNft)
    }

    @Test
    @Disabled //TODO: Fix test
    fun `should convert OrderCancelledEvent`() = runBlocking<Unit> {
        val data = Instant.now()
        val log = log(
            listOf(
                Word.apply("0x6bacc01dbe442496068f7d234edd811f1a5f833243e0aec824f86ab861f3c90d"),
                Word.apply("0x000000000000000000000000bf5e0722c0a175f8762c5b8fcf25c0d5b91b2973"),
                Word.apply("0x0000000000000000000000000000000000000000000000000000000000000000")
            ),
            "0xd05d08e9f1e440f0d77f1b3a69f3716cd181357028b9e5999ea3c3eaf8e0b216"
        )
        val transaction = transaction(
            "0xfd9f1e10000000000000000000000000000000000000000000000000000000000000002000000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000020000000000000000000000000bf5e0722c0a175f8762c5b8fcf25c0d5b91b297300000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000160000000000000000000000000000000000000000000000000000000000000022000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000063169b57ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff300000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000027e1b02166655bf277a5cbbe276f3210000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000002000000000000000000000000c9a89253e814941f58aa1bde5ddc7258f4a33bca000000000000000000000000000000000000000000000000000000000000000c00000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000002540be40000000000000000000000000000000000000000000000000000000002540be400000000000000000000000000bf5e0722c0a175f8762c5b8fcf25c0d5b91b2973"
        )
        val event = OrderCancelledEvent.apply(log)
        val cancels = converter.convert(event, transaction, 0, 1, data)

        assertThat(cancels).hasSize(1)
        val cancel = cancels.single()
        assertThat(cancel.hash).isEqualTo(Word.apply("0xd05d08e9f1e440f0d77f1b3a69f3716cd181357028b9e5999ea3c3eaf8e0b216"))
    }

    private fun transaction(data: String) = Transaction(
        Word.apply(ByteArray(32)), // transactionHash
        BigInteger.ZERO, // nonce
        Word.apply(ByteArray(32)), // blockHash
        BigInteger.ZERO, // blockNumber
        Address.ZERO(), // creates
        BigInteger.ZERO, // transactionIndex
        Address.ZERO(), // from
        Address.ZERO(), // to
        BigInteger.ZERO, // value
        BigInteger.ZERO, // gasPrice
        BigInteger.ZERO, // gas
        Binary.apply(data), // input
    )
}