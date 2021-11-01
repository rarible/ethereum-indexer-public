package com.rarible.protocol.order.api.controller

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.ethereum.common.toHexString
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.AssetTypeDto
import com.rarible.protocol.dto.EthAssetTypeDto
import com.rarible.protocol.dto.OrderCurrenciesDto
import com.rarible.protocol.order.api.data.createErc721Asset
import com.rarible.protocol.order.api.data.createEthAsset
import com.rarible.protocol.order.api.data.createOrderVersion
import com.rarible.protocol.order.api.integration.AbstractIntegrationTest
import com.rarible.protocol.order.api.integration.IntegrationTest
import com.rarible.protocol.order.core.converters.dto.AssetTypeDtoConverter
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.CollectionAssetType
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV1
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.OrderVersion
import io.mockk.coEvery
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.Address
import scalether.domain.AddressFactory
import java.math.BigInteger

@IntegrationTest
class OrderControllerCollectionFt : AbstractIntegrationTest() {

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
        coEvery { assetMakeBalanceProvider.getMakeBalance(any()) } returns EthUInt256.TEN
    }

    @BeforeEach
    fun initializeToken() {
        token = randomAddress()
        tokenId = BigInteger.valueOf(2)
        sellMake = Asset(Erc721AssetType(token, EthUInt256(tokenId)), EthUInt256.ONE)
        bidTake = sellMake
    }

    @Test
    fun `test collection`() = runBlocking<Unit> {
        val maker = randomAddress()
        val make = Asset(EthAssetType, EthUInt256.ONE)
        val take = Asset(CollectionAssetType(token), EthUInt256.ONE)
        val orderV1 = createOrderVersion(make, take).copy(maker = maker)
        saveOrderVersions(orderV1)

        val dto = controller.getOrderBidsByItem(token.hex(), tokenId.toString(), null, null, null, null, null)
        assertNotNull(dto)
    }

    private suspend fun checkSellCurrencies(vararg currencies: Asset) {
        val currenciesDto = controller.getCurrenciesBySellOrdersOfItem(token.prefixed(), tokenId.toString()).body!!
        assertThat(currenciesDto.orderType).isEqualTo(OrderCurrenciesDto.OrderType.SELL)
        assertThat(currenciesDto.currencies.map { it.intern() }).containsExactlyInAnyOrderElementsOf(
            currencies.map { assetTypeDtoConverter.convert(it.type).intern() }
        ).hasSameSizeAs(currencies)
    }

    private suspend fun checkBidCurrencies(vararg currencies: Asset) {
        val currenciesDto = controller.getCurrenciesByBidOrdersOfItem(token.prefixed(), tokenId.toString()).body!!
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

    private fun createOrder(maker: Address, make: Asset, take: Asset, salt: EthUInt256) = Order(
        maker = maker,
        taker = null,
        make = make,
        take = take,
        makeStock = make.value,
        type = OrderType.RARIBLE_V2,
        fill = EthUInt256.ZERO,
        cancelled = false,
        salt = salt,
        start = null,
        end = null,
        data = OrderRaribleV2DataV1(emptyList(), emptyList()),
        signature = null,
        createdAt = nowMillis(),
        lastUpdateAt = nowMillis()
    )
}
