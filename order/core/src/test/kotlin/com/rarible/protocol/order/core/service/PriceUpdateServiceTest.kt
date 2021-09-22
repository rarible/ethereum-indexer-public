package com.rarible.protocol.order.core.service

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.contract.service.ContractService
import com.rarible.ethereum.domain.Blockchain
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.currency.api.client.CurrencyControllerApi
import com.rarible.protocol.currency.dto.BlockchainDto
import com.rarible.protocol.currency.dto.CurrencyRateDto
import com.rarible.protocol.order.core.model.*
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
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
        val usdValue = priceUpdateService.getAssetsUsdValue(makeAsset, takeAsset, nowMillis())
        assertThat(usdValue).isInstanceOf(OrderUsdValue.SellOrder::class.java)
        assertThat(usdValue?.makePriceUsd.toString()).isEqualTo("4000.000000000000000000")
        assertThat(usdValue?.takeUsd.toString()).isEqualTo("40000.000000000000000000")
    }
}
