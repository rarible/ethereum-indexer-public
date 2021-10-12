package com.rarible.protocol.order.core.service

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.contract.service.ContractService
import com.rarible.ethereum.domain.Blockchain
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.currency.api.client.CurrencyControllerApi
import com.rarible.protocol.currency.dto.BlockchainDto
import com.rarible.protocol.currency.dto.CurrencyRateDto
import com.rarible.protocol.order.core.model.*
import io.daonomic.rpc.domain.Word
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import scalether.domain.Address
import scalether.domain.AddressFactory
import java.math.BigDecimal
import java.math.BigInteger

internal class PriceUpdateServiceTest {
    private val contractService = mockk<ContractService>()
    private val normalizer = PriceNormalizer(contractService)
    private val currencyApi = mockk<CurrencyControllerApi>()

    private val priceUpdateService = PriceUpdateService(Blockchain.ETHEREUM, currencyApi, normalizer)

    @BeforeEach
    fun setupRate() {
        coEvery {
            currencyApi.getCurrencyRate(
                eq(BlockchainDto.ETHEREUM),
                eq(Address.ZERO().hex()),
                any()
            )
        } returns Mono.just(
            CurrencyRateDto(
                fromCurrencyId = "ETH",
                toCurrencyId = "USD",
                rate = BigDecimal.valueOf(4000),
                date = nowMillis()
            )
        )
    }

    @Test
    fun `should calculate bid value`() = runBlocking<Unit> {
        val makeAsset = Asset(
            EthAssetType,
            EthUInt256.of(BigInteger.valueOf(10).pow(18) * BigInteger.valueOf(20))
        )
        val takeAsset = Asset(
            Erc721AssetType(AddressFactory.create(), EthUInt256.TEN),
            EthUInt256.ONE
        )

        val usdValue = priceUpdateService.getAssetsUsdValue(makeAsset, takeAsset, nowMillis())
        assertThat(usdValue).isInstanceOf(OrderUsdValue.BidOrder::class.java)
        assertThat(usdValue?.makeUsd.toString()).isEqualTo("80000.000000000000000000")
        assertThat(usdValue?.takePriceUsd.toString()).isEqualTo("80000.000000000000000000")
    }

    @Test
    fun `should calculate sell value`() = runBlocking<Unit> {
        val makeAsset = Asset(
            Erc1155AssetType(AddressFactory.create(), EthUInt256.TEN),
            EthUInt256.TEN
        )
        val takeAsset = Asset(
            EthAssetType,
            EthUInt256.of(BigInteger.valueOf(10).pow(18) * BigInteger.valueOf(10))
        )

        val usdValue = priceUpdateService.getAssetsUsdValue(makeAsset, takeAsset, nowMillis())
        assertThat(usdValue).isInstanceOf(OrderUsdValue.SellOrder::class.java)
        assertThat(usdValue?.makePriceUsd.toString()).isEqualTo("4000.000000000000000000")
        assertThat(usdValue?.takeUsd.toString()).isEqualTo("40000.000000000000000000")
    }

    @Test
    fun `should calculate make price`() = runBlocking<Unit> {
        val makeAsset = Asset(
            Erc1155AssetType(AddressFactory.create(), EthUInt256.TEN),
            EthUInt256.TEN
        )
        val takeAsset = Asset(
            EthAssetType,
            EthUInt256.of(BigInteger.valueOf(10).pow(18) * BigInteger.valueOf(10))
        )
        val orderV = createOrderVersion(makeAsset, takeAsset)
        val calculatedPrice = priceUpdateService.withUpdatedAllPrices(orderV)

        assertEquals(BigDecimal("1.000000000000000000"), calculatedPrice.makePrice)
    }

    @Test
    fun `should calculate take price`() = runBlocking<Unit> {
        val makeAsset = Asset(
            EthAssetType,
            EthUInt256.of(BigInteger.valueOf(10).pow(18) * BigInteger.valueOf(20))
        )
        val takeAsset = Asset(
            Erc721AssetType(AddressFactory.create(), EthUInt256.TEN),
            EthUInt256.ONE
        )
        val orderV = createOrderVersion(makeAsset, takeAsset)
        val calculatedPrice = priceUpdateService.withUpdatedAllPrices(orderV)

        assertEquals(BigDecimal("20.000000000000000000"), calculatedPrice.takePrice)
    }

    private fun createOrderVersion(make: Asset, take: Asset) = OrderVersion(
        hash = Word.apply(RandomUtils.nextBytes(32)),
        maker = AddressFactory.create(),
        taker = AddressFactory.create(),
        makePriceUsd = (1..100).random().toBigDecimal(),
        takePriceUsd = (1..100).random().toBigDecimal(),
        makePrice = null,
        takePrice = null,
        makeUsd = (1..100).random().toBigDecimal(),
        takeUsd = (1..100).random().toBigDecimal(),
        make = make,
        take = take,
        platform = Platform.RARIBLE,
        type = OrderType.RARIBLE_V2,
        salt = EthUInt256.TEN,
        start = null,
        end = null,
        data = OrderRaribleV2DataV1(emptyList(), emptyList()),
        signature = null
    )
}
