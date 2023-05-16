package com.rarible.protocol.order.api.controller

import com.rarible.core.test.data.randomAddress
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.AssetTypeDto
import com.rarible.protocol.dto.EthAssetTypeDto
import com.rarible.protocol.dto.OrderCurrenciesDto
import com.rarible.protocol.dto.OrderStatusDto
import com.rarible.protocol.order.api.data.createErc721Asset
import com.rarible.protocol.order.api.data.createEthAsset
import com.rarible.protocol.order.api.data.createOrderVersion
import com.rarible.protocol.order.api.integration.AbstractIntegrationTest
import com.rarible.protocol.order.api.integration.IntegrationTest
import com.rarible.protocol.order.core.converters.dto.AssetTypeDtoConverter
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.CollectionAssetType
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.MakeBalanceState
import com.rarible.protocol.order.core.model.OrderVersion
import io.mockk.coEvery
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.Address
import java.math.BigInteger

@IntegrationTest
class OrderControllerCurrenciesIt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var controller: OrderController

    @Autowired
    private lateinit var assetTypeDtoConverter: AssetTypeDtoConverter

    private lateinit var token: Address
    private lateinit var tokenId: BigInteger
    private lateinit var sellMake: Asset
    private lateinit var bidTake: Asset

    @BeforeEach
    fun mockMakeBalance() {
        // Make all orders have status ACTIVE
        coEvery { assetMakeBalanceProvider.getMakeBalance(any()) } returns MakeBalanceState(EthUInt256.TEN)
    }

    @BeforeEach
    fun initializeToken() {
        token = randomAddress()
        tokenId = BigInteger.valueOf(2)
        sellMake = Asset(Erc721AssetType(token, EthUInt256(tokenId)), EthUInt256.ONE)
        bidTake = sellMake
    }

    @Test
    fun `sell currencies - two currencies`() = runBlocking<Unit> {
        val currency1 = Asset(EthAssetType, EthUInt256.ONE)
        val currency2 = Asset(Erc721AssetType(randomAddress(), EthUInt256.ONE), EthUInt256.ONE)
        saveOrderVersions(
            createOrderVersion(sellMake, currency1),
            createOrderVersion(sellMake, currency2)
        )
        checkSellCurrencies(listOf(currency1, currency2))
    }

    @Test
    fun `sell currencies - ignore duplicates`() = runBlocking<Unit> {
        val currency1 = Asset(EthAssetType, EthUInt256.ONE)
        val currency2 = Asset(Erc721AssetType(randomAddress(), EthUInt256.ONE), EthUInt256.ONE)
        saveOrderVersions(
            createOrderVersion(sellMake, currency1),
            createOrderVersion(sellMake, currency2),
            createOrderVersion(sellMake, currency2) // Order with the same currency
        )
        checkSellCurrencies(listOf(currency1, currency2))
    }

    @Test
    fun `sell currencies - ignore irrelevant orders`() = runBlocking<Unit> {
        val currency = Asset(Erc721AssetType(randomAddress(), EthUInt256.ONE), EthUInt256.ONE)
        saveOrderVersions(
            createOrderVersion(sellMake, currency),
            createOrderVersion(currency, sellMake),
            createOrderVersion(createErc721Asset(), createEthAsset())
        )
        checkSellCurrencies(listOf(currency))
    }

    @Test
    fun `sell currencies - filtered by status`() = runBlocking<Unit> {
        val currency = Asset(Erc721AssetType(randomAddress(), EthUInt256.ONE), EthUInt256.ONE)
        val inactiveCurrency = Asset(EthAssetType, EthUInt256.ONE)
        saveOrderVersions(createOrderVersion(sellMake, currency))

        val inactiveVersion = createOrderVersion(sellMake, inactiveCurrency)
        io.mockk.clearMocks(assetMakeBalanceProvider)
        coEvery { assetMakeBalanceProvider.getMakeBalance(any()) } returns MakeBalanceState(EthUInt256.ZERO)
        saveOrderVersions(inactiveVersion)

        checkSellCurrencies(listOf(currency), listOf(OrderStatusDto.ACTIVE))
        checkSellCurrencies(listOf(inactiveCurrency), listOf(OrderStatusDto.INACTIVE))
    }

    @Test
    fun `bid currencies - two currencies`() = runBlocking<Unit> {
        val currency1 = Asset(EthAssetType, EthUInt256.ONE)
        val currency2 = Asset(Erc721AssetType(randomAddress(), EthUInt256.ONE), EthUInt256.ONE)
        saveOrderVersions(
            createOrderVersion(currency1, bidTake),
            createOrderVersion(currency2, bidTake)
        )
        checkBidCurrencies(listOf(currency1, currency2))
    }

    @Test
    fun `bid currencies - ignore duplicates`() = runBlocking<Unit> {
        val currency1 = Asset(EthAssetType, EthUInt256.ONE)
        val currency2 = Asset(Erc721AssetType(randomAddress(), EthUInt256.ONE), EthUInt256.ONE)
        saveOrderVersions(
            createOrderVersion(currency1, bidTake),
            createOrderVersion(currency2, bidTake),
            createOrderVersion(currency2, bidTake) // Order with the same currency
        )
        checkBidCurrencies(listOf(currency1, currency2))
    }

    @Test
    fun `bid currencies - ignore irrelevant orders`() = runBlocking<Unit> {
        val currency = Asset(Erc721AssetType(randomAddress(), EthUInt256.ONE), EthUInt256.ONE)
        saveOrderVersions(
            createOrderVersion(currency, bidTake),
            createOrderVersion(bidTake, currency),
            createOrderVersion(createErc721Asset(), createEthAsset())
        )
        checkBidCurrencies(listOf(currency))
    }

    @Test
    fun `bid currencies - filtered by status`() = runBlocking<Unit> {
        val currency = Asset(Erc721AssetType(randomAddress(), EthUInt256.ONE), EthUInt256.ONE)
        val inactiveCurrency = Asset(EthAssetType, EthUInt256.ONE)
        saveOrderVersions(createOrderVersion(currency, bidTake))

        val inactiveVersion = createOrderVersion(inactiveCurrency, bidTake)
        io.mockk.clearMocks(assetMakeBalanceProvider)
        coEvery { assetMakeBalanceProvider.getMakeBalance(any()) } returns MakeBalanceState(EthUInt256.ZERO)
        saveOrderVersions(inactiveVersion)

        checkBidCurrencies(listOf(currency), listOf(OrderStatusDto.ACTIVE))
        checkBidCurrencies(listOf(inactiveCurrency), listOf(OrderStatusDto.INACTIVE))
    }

    @Test
    fun `sell currencies - collection asset`() = runBlocking<Unit> {
        val asset = Asset(CollectionAssetType(token), EthUInt256.ONE)
        val currency = Asset(EthAssetType, EthUInt256.ONE)
        saveOrderVersions(createOrderVersion(asset, currency))
        checkSellCurrencies(listOf(currency))
    }

    @Test
    fun `bid currencies - collection asset`() = runBlocking<Unit> {
        val asset = Asset(CollectionAssetType(token), EthUInt256.ONE)
        val currency = Asset(EthAssetType, EthUInt256.ONE)
        saveOrderVersions(createOrderVersion(currency, asset))
        checkBidCurrencies(listOf(currency))
    }

    private suspend fun checkSellCurrencies(currencies: List<Asset>, statuses: List<OrderStatusDto> = emptyList()) {
        val currenciesDto = controller.getCurrenciesBySellOrdersOfItem(
            token.prefixed(),
            tokenId.toString(),
            statuses
        ).body!!
        assertThat(currenciesDto.orderType).isEqualTo(OrderCurrenciesDto.OrderType.SELL)
        assertThat(currenciesDto.currencies.map { it.intern() }).containsExactlyInAnyOrderElementsOf(
            currencies.map { assetTypeDtoConverter.convert(it.type).intern() }
        ).hasSameSizeAs(currencies)
    }

    private suspend fun checkBidCurrencies(currencies: List<Asset>, statuses: List<OrderStatusDto> = emptyList()) {
        val currenciesDto = controller.getCurrenciesByBidOrdersOfItem(
            token.prefixed(),
            tokenId.toString(),
            statuses
        ).body!!
        assertThat(currenciesDto.orderType).isEqualTo(OrderCurrenciesDto.OrderType.BID)
        assertThat(currenciesDto.currencies.map { it.intern() }).containsExactlyInAnyOrderElementsOf(
            currencies.map { assetTypeDtoConverter.convert(it.type).intern() }
        ).hasSameSizeAs(currencies)
    }

    // Workaround for equals() of EthAssetTypeDto() != EthAssetTypeDto()
    private val ethAssetTypeDto = EthAssetTypeDto()
    private fun AssetTypeDto.intern() = if (this is EthAssetTypeDto) ethAssetTypeDto else this

    private suspend fun saveOrderVersions(vararg order: OrderVersion) {
        order.forEach { orderUpdateService.save(it) }
    }
}
