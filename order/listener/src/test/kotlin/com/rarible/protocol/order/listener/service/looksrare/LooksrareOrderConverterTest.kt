package com.rarible.protocol.order.listener.service.looksrare

import com.rarible.core.test.data.randomAddress
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.Erc1155AssetType
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.OrderLooksrareDataV1
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.service.PriceUpdateService
import com.rarible.protocol.order.listener.data.randomLooksrareOrder
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigInteger

internal class LooksrareOrderConverterTest {
    private val priceUpdateService = mockk<PriceUpdateService> {
        coEvery { withUpdatedPrices(any<OrderVersion>()) } answers { it.invocation.args.first() as OrderVersion }
    }
    private val currencyAddresses = OrderIndexerProperties.CurrencyContractAddresses(weth = randomAddress())

    private val converter = LooksrareOrderConverter(
        priceUpdateService = priceUpdateService,
        currencyAddresses = currencyAddresses
    )

    @Test
    fun `should convert erc721 sell order with weth currency`() = runBlocking<Unit> {
        val looksrareOrder = randomLooksrareOrder().copy(
            isOrderAsk = true,
            amount = BigInteger.ONE
        )
        val orderVersion = converter.convert(looksrareOrder)
        assertThat(orderVersion).isNotNull
        assertThat(orderVersion!!.hash).isEqualTo(looksrareOrder.hash)
        assertThat(orderVersion.maker).isEqualTo(looksrareOrder.signer)
        assertThat(orderVersion.taker).isNull()
        assertThat(orderVersion.make.value.value).isEqualTo(looksrareOrder.amount)
        assertThat(orderVersion.make.type).isInstanceOf(Erc721AssetType::class.java)
        assertThat((orderVersion.make.type as Erc721AssetType).token).isEqualTo(looksrareOrder.collectionAddress)
        assertThat((orderVersion.make.type as Erc721AssetType).tokenId.value).isEqualTo(looksrareOrder.tokenId)
        assertThat(orderVersion.take.value.value).isEqualTo(looksrareOrder.price)
        assertThat(orderVersion.take.type).isInstanceOf(Erc20AssetType::class.java)
        assertThat((orderVersion.take.type as Erc20AssetType).token).isEqualTo(looksrareOrder.currencyAddress)
        assertThat(orderVersion.type).isEqualTo(OrderType.LOOKSRARE)
        assertThat(orderVersion.salt).isEqualTo(EthUInt256.ZERO)
        assertThat(orderVersion.start).isEqualTo(looksrareOrder.startTime.epochSecond)
        assertThat(orderVersion.end).isEqualTo(looksrareOrder.endTime.epochSecond)
        assertThat(orderVersion.createdAt).isEqualTo(looksrareOrder.startTime)
        assertThat(orderVersion.signature).isEqualTo(looksrareOrder.signature)
        assertThat(orderVersion.data).isInstanceOf(OrderLooksrareDataV1::class.java)
        with(orderVersion.data as OrderLooksrareDataV1) {
            assertThat(minPercentageToAsk).isEqualTo(looksrareOrder.minPercentageToAsk)
            assertThat(params).isEqualTo(looksrareOrder.params)
            assertThat(nonce).isEqualTo(looksrareOrder.nonce)
            assertThat(strategy).isEqualTo(looksrareOrder.strategy)
        }
    }

    @Test
    fun `should convert erc155 sell order with weth currency`() = runBlocking<Unit> {
        val looksrareOrder = randomLooksrareOrder().copy(
            isOrderAsk = true,
            amount = BigInteger.TEN
        )
        val orderVersion = converter.convert(looksrareOrder)
        assertThat(orderVersion!!.make.type).isInstanceOf(Erc1155AssetType::class.java)
        assertThat((orderVersion.make.type as Erc1155AssetType).token).isEqualTo(looksrareOrder.collectionAddress)
        assertThat((orderVersion.make.type as Erc1155AssetType).tokenId.value).isEqualTo(looksrareOrder.tokenId)
    }

    @Test
    fun `should convert sell order with weth currency`() = runBlocking<Unit> {
        val looksrareOrder = randomLooksrareOrder().copy(
            currencyAddress = currencyAddresses.weth,
            isOrderAsk = true,
            amount = BigInteger.TEN
        )
        val orderVersion = converter.convert(looksrareOrder)
        assertThat(orderVersion!!.take.type).isInstanceOf(EthAssetType::class.java)
    }

    @Test
    fun `should convert bid eerc721 order`() = runBlocking<Unit> {
        val looksrareOrder = randomLooksrareOrder().copy(
            isOrderAsk = false,
            amount = BigInteger.ONE
        )
        val orderVersion = converter.convert(looksrareOrder)

        assertThat(orderVersion.make.value.value).isEqualTo(looksrareOrder.price)
        assertThat(orderVersion.make.type).isInstanceOf(Erc20AssetType::class.java)
        assertThat((orderVersion.make.type as Erc20AssetType).token).isEqualTo(looksrareOrder.currencyAddress)
        assertThat(orderVersion!!.take.value.value).isEqualTo(looksrareOrder.amount)
        assertThat(orderVersion.take.type).isInstanceOf(Erc721AssetType::class.java)
        assertThat((orderVersion.take.type as Erc721AssetType).token).isEqualTo(looksrareOrder.collectionAddress)
        assertThat((orderVersion.take.type as Erc721AssetType).tokenId.value).isEqualTo(looksrareOrder.tokenId)
    }
}