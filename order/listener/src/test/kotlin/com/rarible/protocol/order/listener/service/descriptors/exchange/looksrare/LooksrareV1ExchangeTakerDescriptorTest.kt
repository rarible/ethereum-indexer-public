package com.rarible.protocol.order.listener.service.descriptors.exchange.looksrare

import com.rarible.core.contract.model.Erc20Token
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.core.test.data.randomAddress
import com.rarible.ethereum.contract.service.ContractService
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.data.randomBidOrderUsdValue
import com.rarible.protocol.order.core.data.randomSellOrderUsdValue
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.OrderSide
import com.rarible.protocol.order.core.model.TokenStandard
import com.rarible.protocol.order.core.service.PriceNormalizer
import com.rarible.protocol.order.core.service.PriceUpdateService
import com.rarible.protocol.order.listener.data.log
import com.rarible.protocol.order.listener.service.looksrare.TokenStandardProvider
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import reactor.kotlin.core.publisher.toFlux
import scalether.domain.Address
import scalether.domain.response.Transaction
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit

internal class LooksrareV1ExchangeTakerDescriptorTest {
    private val looksrareTakeEventMetric = mockk<RegisteredCounter> {
        every { increment() } returns Unit
    }
    private val wrapperLooksrareMetric = mockk<RegisteredCounter> {
        every { increment() } returns Unit
    }
    private val currencyContractAddresses = OrderIndexerProperties.CurrencyContractAddresses(
        weth = Address.apply("0xc778417e063141139fce010982780140aa0cd5ab")
    )
    private val exchangeContractAddresses = mockk<OrderIndexerProperties.ExchangeContractAddresses>()
    private val tokenStandardProvider = mockk<TokenStandardProvider>()
    private val priceUpdateService = mockk<PriceUpdateService>()
    private val contractService = mockk<ContractService>()
    private val prizeNormalizer = PriceNormalizer(contractService)

    private val descriptorBid = LooksrareV1ExchangeTakerBidDescriptor(
        looksrareTakeEventMetric,
        wrapperLooksrareMetric,
        tokenStandardProvider,
        priceUpdateService,
        prizeNormalizer,
        exchangeContractAddresses,
        currencyContractAddresses
    )
    private val descriptorAsk = LooksrareV1ExchangeTakerAskDescriptor(
        looksrareTakeEventMetric,
        wrapperLooksrareMetric,
        tokenStandardProvider,
        priceUpdateService,
        prizeNormalizer,
        exchangeContractAddresses,
        currencyContractAddresses
    )

    @Test
    fun `should convert erc721 sell event`() = runBlocking<Unit> {
        val transaction = mockk<Transaction> { every { input() } returns Binary.empty() }
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
        coEvery { priceUpdateService.getAssetsUsdValue(make = expectedNft, take = expectedPayment, at = data) } returns sellOrderUsd
        coEvery { priceUpdateService.getAssetsUsdValue(make = expectedPayment, take = expectedNft, at = data) } returns bidOrderUsd
        coEvery { tokenStandardProvider.getTokenStandard(nftAssetType.token) } returns TokenStandard.ERC721

        val matches = descriptorBid.convert(log, transaction, data.epochSecond, 0, 0).toFlux().collectList().awaitFirst()

        Assertions.assertThat(matches).hasSize(2)
        val left = matches[0]
        Assertions.assertThat(left.hash).isEqualTo(Word.apply("0x25765a7f3ffe3496f5118a663a8fbc88fd836d05aaf125e8cdac4fea0e9bd4ce"))
        Assertions.assertThat(left.side).isEqualTo(OrderSide.LEFT)
        Assertions.assertThat(left.maker).isEqualTo(Address.apply("0x6c8ba1dafb22eae61e9cd3da724cbc3d164c27b9"))
        Assertions.assertThat(left.taker).isEqualTo(Address.apply("0x47921676A46CcFe3D80b161c7B4DDC8Ed9e716B6"))
        Assertions.assertThat(left.make).isEqualTo(expectedNft)
        Assertions.assertThat(left.take).isEqualTo(expectedPayment)
        Assertions.assertThat(left.fill).isEqualTo(expectedNft.value)
        Assertions.assertThat(left.date).isEqualTo(data)
        Assertions.assertThat(left.makeUsd).isEqualTo(sellOrderUsd.makeUsd)
        Assertions.assertThat(left.takeUsd).isEqualTo(sellOrderUsd.takeUsd)
        Assertions.assertThat(left.makePriceUsd).isEqualTo(sellOrderUsd.makePriceUsd)
        Assertions.assertThat(left.takePriceUsd).isEqualTo(sellOrderUsd.takePriceUsd)
        Assertions.assertThat(left.takeValue).isEqualTo(BigDecimal("0.010000000000000000"))
        Assertions.assertThat(left.makeValue).isEqualTo(BigDecimal.ONE)
        Assertions.assertThat(left.source).isEqualTo(HistorySource.LOOKSRARE)
        Assertions.assertThat(left.adhoc).isFalse()
        Assertions.assertThat(left.counterAdhoc).isTrue()
        val right = matches[1]
        Assertions.assertThat(right.counterHash).isEqualTo(Word.apply("0x25765a7f3ffe3496f5118a663a8fbc88fd836d05aaf125e8cdac4fea0e9bd4ce"))
        Assertions.assertThat(right.side).isEqualTo(OrderSide.RIGHT)
        Assertions.assertThat(right.maker).isEqualTo(Address.apply("0x47921676A46CcFe3D80b161c7B4DDC8Ed9e716B6"))
        Assertions.assertThat(right.taker).isEqualTo(Address.apply("0x6c8ba1dafb22eae61e9cd3da724cbc3d164c27b9"))
        Assertions.assertThat(right.make).isEqualTo(expectedPayment)
        Assertions.assertThat(right.take).isEqualTo(expectedNft)
        Assertions.assertThat(right.fill).isEqualTo(expectedNft.value)
        Assertions.assertThat(right.date).isEqualTo(data)
        Assertions.assertThat(right.makeUsd).isEqualTo(bidOrderUsd.makeUsd)
        Assertions.assertThat(right.takeUsd).isEqualTo(bidOrderUsd.takeUsd)
        Assertions.assertThat(right.makePriceUsd).isEqualTo(bidOrderUsd.makePriceUsd)
        Assertions.assertThat(right.takePriceUsd).isEqualTo(bidOrderUsd.takePriceUsd)
        Assertions.assertThat(right.makeValue).isEqualTo(BigDecimal("0.010000000000000000"))
        Assertions.assertThat(right.takeValue).isEqualTo(BigDecimal.ONE)
        Assertions.assertThat(right.source).isEqualTo(HistorySource.LOOKSRARE)
        Assertions.assertThat(left.adhoc).isFalse()
        Assertions.assertThat(left.counterAdhoc).isTrue()
    }

    @Test
    fun `should convert erc721 bid event`() = runBlocking<Unit> {
        val transaction = mockk<Transaction> { every { input() } returns Binary.empty() }
        val data = Instant.now().truncatedTo(ChronoUnit.SECONDS)
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
        coEvery { priceUpdateService.getAssetsUsdValue(make = expectedNft, take = expectedPayment, at = data) } returns sellOrderUsd
        coEvery { priceUpdateService.getAssetsUsdValue(make = expectedPayment, take = expectedNft, at = data) } returns bidOrderUsd
        coEvery { tokenStandardProvider.getTokenStandard(nftAssetType.token) } returns TokenStandard.ERC721

        val matches = descriptorAsk.convert(log, transaction, data.epochSecond, 0, 0).toFlux().collectList().awaitFirst()

        Assertions.assertThat(matches).hasSize(2)
        val left = matches[0]
        Assertions.assertThat(left.hash).isEqualTo(Word.apply("0xa04ce3cc6721c3a7882a76705f77b4ce17a006b7cec01ac5a033f36f08184772"))
        Assertions.assertThat(left.side).isEqualTo(OrderSide.LEFT)
        Assertions.assertThat(left.maker).isEqualTo(Address.apply("0x6c8ba1dafb22eae61e9cd3da724cbc3d164c27b9"))
        Assertions.assertThat(left.taker).isEqualTo(Address.apply("0x47921676A46CcFe3D80b161c7B4DDC8Ed9e716B6"))
        Assertions.assertThat(left.make).isEqualTo(expectedPayment)
        Assertions.assertThat(left.take).isEqualTo(expectedNft)
        Assertions.assertThat(left.fill).isEqualTo(expectedNft.value)
        Assertions.assertThat(left.date).isEqualTo(data)
        Assertions.assertThat(left.makeUsd).isEqualTo(bidOrderUsd.makeUsd)
        Assertions.assertThat(left.takeUsd).isEqualTo(bidOrderUsd.takeUsd)
        Assertions.assertThat(left.makePriceUsd).isEqualTo(bidOrderUsd.makePriceUsd)
        Assertions.assertThat(left.takePriceUsd).isEqualTo(bidOrderUsd.takePriceUsd)
        Assertions.assertThat(left.makeValue).isEqualTo(BigDecimal("0.020000000000000000"))
        Assertions.assertThat(left.takeValue).isEqualTo(BigDecimal.ONE)
        Assertions.assertThat(left.source).isEqualTo(HistorySource.LOOKSRARE)
        Assertions.assertThat(left.adhoc).isFalse()
        Assertions.assertThat(left.counterAdhoc).isTrue()
        val right = matches[1]
        Assertions.assertThat(right.counterHash).isEqualTo(Word.apply("0xa04ce3cc6721c3a7882a76705f77b4ce17a006b7cec01ac5a033f36f08184772"))
        Assertions.assertThat(right.side).isEqualTo(OrderSide.RIGHT)
        Assertions.assertThat(right.maker).isEqualTo(Address.apply("0x47921676A46CcFe3D80b161c7B4DDC8Ed9e716B6"))
        Assertions.assertThat(right.taker).isEqualTo(Address.apply("0x6c8ba1dafb22eae61e9cd3da724cbc3d164c27b9"))
        Assertions.assertThat(right.make).isEqualTo(expectedNft)
        Assertions.assertThat(right.take).isEqualTo(expectedPayment)
        Assertions.assertThat(right.fill).isEqualTo(expectedNft.value)
        Assertions.assertThat(right.date).isEqualTo(data)
        Assertions.assertThat(right.makeUsd).isEqualTo(sellOrderUsd.makeUsd)
        Assertions.assertThat(right.takeUsd).isEqualTo(sellOrderUsd.takeUsd)
        Assertions.assertThat(right.makePriceUsd).isEqualTo(sellOrderUsd.makePriceUsd)
        Assertions.assertThat(right.takePriceUsd).isEqualTo(sellOrderUsd.takePriceUsd)
        Assertions.assertThat(right.takeValue).isEqualTo(BigDecimal("0.020000000000000000"))
        Assertions.assertThat(right.makeValue).isEqualTo(BigDecimal.ONE)
        Assertions.assertThat(right.source).isEqualTo(HistorySource.LOOKSRARE)
        Assertions.assertThat(left.adhoc).isFalse()
        Assertions.assertThat(left.counterAdhoc).isTrue()
    }
}