package com.rarible.protocol.order.listener.service.opensea

import com.rarible.core.contract.model.Erc20Token
import com.rarible.core.test.data.randomAddress
import com.rarible.ethereum.contract.service.ContractService
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.seaport.v1.events.OrderFulfilledEvent
import com.rarible.protocol.order.core.data.randomBidOrderUsdValue
import com.rarible.protocol.order.core.data.randomSellOrderUsdValue
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.OrderSide
import com.rarible.protocol.order.core.service.PriceNormalizer
import com.rarible.protocol.order.core.service.PriceUpdateService
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import scalether.domain.Address
import scalether.domain.response.Log
import scalether.java.Lists
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant

internal class SeaportEventConverterTest {
    private val priceUpdateService = mockk<PriceUpdateService>()
    private val contractService = mockk<ContractService>()
    private val prizeNormalizer = PriceNormalizer(contractService)

    private val converter = SeaportEventConverter(
        priceUpdateService,
        prizeNormalizer
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
        val matches = converter.convert(event, data)

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
        val matches = converter.convert(event, data)

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
        assertThat(left.adhoc).isFalse()
        assertThat(left.counterAdhoc).isTrue()
    }

    private fun log(topics: List<Word>,data: String) = Log(
        BigInteger.ONE, // logIndex
        BigInteger.TEN, // transactionIndex
        Word.apply(ByteArray(32)), // transactionHash
        Word.apply(ByteArray(32)), // blockHash
        BigInteger.ZERO, // blockNumber
        Address.ZERO(), // address
        Binary.apply( // data
            data
        ),
        false, // removed
        Lists.toScala( // topics
            topics
        ),
        "" // type
    )
}