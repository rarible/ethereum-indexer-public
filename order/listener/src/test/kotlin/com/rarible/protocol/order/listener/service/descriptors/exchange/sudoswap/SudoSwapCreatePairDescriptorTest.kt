package com.rarible.protocol.order.listener.service.descriptors.exchange.sudoswap

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigDecimal
import com.rarible.core.test.data.randomWord
import com.rarible.ethereum.common.keccak256
import com.rarible.ethereum.contract.service.ContractService
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.AmmNftAssetType
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.OrderSudoSwapAmmDataV1
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.model.SudoSwapPoolType
import com.rarible.protocol.order.core.service.PriceNormalizer
import com.rarible.protocol.order.core.service.PriceUpdateService
import com.rarible.protocol.order.core.trace.TraceCallService
import com.rarible.protocol.order.listener.data.log
import com.rarible.protocol.order.listener.service.sudoswap.SudoSwapEventConverter
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import reactor.kotlin.core.publisher.toFlux
import scalether.domain.Address
import scalether.domain.response.Transaction
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.time.temporal.ChronoUnit

internal class SudoSwapCreatePairDescriptorTest {
    private val sudoswap = randomAddress()
    private val addresses = mockk<OrderIndexerProperties.ExchangeContractAddresses> {
        every { sudoswapPairFactoryV1 } returns sudoswap
    }
    private val traceCallService = TraceCallService(mockk(), mockk())
    private val sudoSwapEventConverter = SudoSwapEventConverter(traceCallService)
    private val contractService = mockk<ContractService>()
    private val priceNormalizer = PriceNormalizer(contractService)
    private val priceUpdateService = mockk<PriceUpdateService>()

    private val descriptor = SudoSwapCreatePairDescriptor(
        exchangeContractAddresses = addresses,
        sudoSwapEventConverter = sudoSwapEventConverter,
        priceUpdateService = priceUpdateService,
        priceNormalizer = priceNormalizer,
    )

    @Test
    fun `should convert eth nft pair`() = runBlocking<Unit> {
        // from: https://etherscan.io/tx/0x2e60979d75c0799d4af59797ac63d148720fc81a5b04c07403439e3d3d68d853
        val transaction = mockk<Transaction> {
            every { hash() } returns Word.apply(randomWord())
            every { from() } returns randomAddress()
            every { to() } returns randomAddress()
            every { input() } returns Binary.apply("0xce9c095d000000000000000000000000ef1a89cbfabe59397ffda11fc5df293e9bc5db900000000000000000000000005b6ac51d9b1cede0068a1b26533cace807f883ee0000000000000000000000002a3b53e1ce8cb9f3290e9ba70033951f07c686f30000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000002386f26fc1000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000447af67e190834800000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000120f")
            every { value() } returns BigInteger.ZERO
        }
        val date = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val log = log(
            listOf(
                Word.apply("0xf5bdc103c3e68a20d5f97d2d46792d3fdddfa4efeb6761f8141e6a7b936ca66c")
            ),
            "0x00000000000000000000000023a46b04d72d9ad624e99fb432c5a9ce212ac2f7"
        )
        val expectedNftAsset = Asset(AmmNftAssetType(Address.apply("0xeF1a89cbfAbE59397FfdA11Fc5DF293E9bC5Db90")), EthUInt256.ONE)
        val expectedCurrencyAsset = Asset(EthAssetType, EthUInt256.ZERO)
        val expectedData = OrderSudoSwapAmmDataV1(
            poolAddress = Address.apply("0x23a46b04d72d9ad624e99fb432c5a9ce212ac2f7"),
            bondingCurve = Address.apply("0x5B6aC51d9B1CeDE0068a1B26533CAce807f883Ee"),
            assetRecipient = Address.apply("0x2a3B53e1Ce8CB9f3290e9Ba70033951F07c686f3"),
            poolType = SudoSwapPoolType.NFT,
            delta = BigInteger("10000000000000000"),
            fee = BigInteger.ZERO
        )
        val expectedPrice = EthUInt256.of("308407960199005000")

        coEvery { priceUpdateService.getAssetUsdValue(expectedNftAsset.type, expectedPrice.value, date) } returns BigDecimal.valueOf(3)

        val onChainAmmOrder = descriptor.convert(log, transaction, date.epochSecond, 0, 1).toFlux().awaitSingle()
        assertThat(onChainAmmOrder.maker).isEqualTo(Address.apply("0x23a46b04d72d9ad624e99fb432c5a9ce212ac2f7"))
        assertThat(onChainAmmOrder.make).isEqualTo(expectedNftAsset)
        assertThat(onChainAmmOrder.take).isEqualTo(expectedCurrencyAsset)
        assertThat(onChainAmmOrder.createdAt).isEqualTo(date)
        assertThat(onChainAmmOrder.platform).isEqualTo(Platform.SUDOSWAP)
        assertThat(onChainAmmOrder.data).isEqualTo(expectedData)
        assertThat(onChainAmmOrder.inNft).isEqualTo(listOf(EthUInt256.of(4623)))
        assertThat(onChainAmmOrder.price).isEqualTo(expectedPrice)
        assertThat(onChainAmmOrder.priceValue).isEqualTo(BigDecimal("0.308407960199005000"))
        assertThat(onChainAmmOrder.priceUsd).isEqualTo(BigDecimal.valueOf(3))
        assertThat(onChainAmmOrder.hash).isEqualTo(keccak256(expectedData.poolAddress))
        assertThat(onChainAmmOrder.date).isEqualTo(date)
        assertThat(onChainAmmOrder.source).isEqualTo(HistorySource.SUDOSWAP)
    }

    @Test
    fun `should convert eth trade pair`() = runBlocking<Unit> {
        // from: https://etherscan.io/tx/0x2afd43baff8d021a707f19a6731e7838105bdf21a4ec5a8cbc156741403b6e85
        val transaction = mockk<Transaction> {
            every { hash() } returns Word.apply(randomWord())
            every { from() } returns randomAddress()
            every { to() } returns randomAddress()
            every { input() } returns Binary.apply("0xce9c095d000000000000000000000000ef1a89cbfabe59397ffda11fc5df293e9bc5db900000000000000000000000005b6ac51d9b1cede0068a1b26533cace807f883ee00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000002000000000000000000000000000000000000000000000000016345785d8a000000000000000000000000000000000000000000000000000000470de4df8200000000000000000000000000000000000000000000000000000359d9b23f856a3800000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000004000000000000000000000000000000000000000000000000000000000000144f000000000000000000000000000000000000000000000000000000000000144c000000000000000000000000000000000000000000000000000000000000144d000000000000000000000000000000000000000000000000000000000000135c")
            every { value() } returns BigInteger("3787004998077662")
        }
        val date = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val log = log(
            listOf(
                Word.apply("0xf5bdc103c3e68a20d5f97d2d46792d3fdddfa4efeb6761f8141e6a7b936ca66c")
            ),
            "0x00000000000000000000000056b69cbcbac832a3a1c8c4f195654a610f96777b"
        )
        val expectedNftAsset = Asset(AmmNftAssetType(Address.apply("0xeF1a89cbfAbE59397FfdA11Fc5DF293E9bC5Db90")), EthUInt256.of(4))
        val expectedCurrencyAsset = Asset(EthAssetType, EthUInt256.of("3787004998077662"))
        val expectedData = OrderSudoSwapAmmDataV1(
            poolAddress = Address.apply("0x56b69cbcbac832a3a1c8c4f195654a610f96777b"),
            bondingCurve = Address.apply("0x5B6aC51d9B1CeDE0068a1B26533CAce807f883Ee"),
            assetRecipient = Address.apply("0x0000000000000000000000000000000000000000"),
            poolType = SudoSwapPoolType.TRADE,
            delta = BigInteger("100000000000000000"),
            fee = BigInteger("20000000000000000")
        )
        val expectedPrice = EthUInt256.of("241463414634146360")

        coEvery { priceUpdateService.getAssetUsdValue(expectedNftAsset.type, expectedPrice.value, date) } returns randomBigDecimal()

        val onChainAmmOrder = descriptor.convert(log, transaction, date.epochSecond, 0, 1).toFlux().awaitSingle()
        assertThat(onChainAmmOrder.maker).isEqualTo(Address.apply("0x56b69cbcbac832a3a1c8c4f195654a610f96777b"))
        assertThat(onChainAmmOrder.make).isEqualTo(expectedNftAsset)
        assertThat(onChainAmmOrder.take).isEqualTo(expectedCurrencyAsset)
        assertThat(onChainAmmOrder.data).isEqualTo(expectedData)
        assertThat(onChainAmmOrder.inNft).containsExactlyInAnyOrder(
            EthUInt256.of(5199),
            EthUInt256.of(5196),
            EthUInt256.of(5197),
            EthUInt256.of(4956),
        )
        assertThat(onChainAmmOrder.price).isEqualTo(expectedPrice)
    }


}