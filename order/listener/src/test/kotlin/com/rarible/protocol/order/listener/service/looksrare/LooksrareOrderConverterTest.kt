package com.rarible.protocol.order.listener.service.looksrare

import com.rarible.core.test.data.randomAddress
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.looksrare.client.model.v2.CollectionType
import com.rarible.looksrare.client.model.v2.QuoteType
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.Erc1155AssetType
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.OrderLooksrareDataV2
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.service.PriceUpdateService
import com.rarible.protocol.order.listener.data.randomLooksrareOrder
import com.rarible.protocol.order.core.metric.ForeignOrderMetrics
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigInteger

internal class LooksrareOrderConverterTest {

    private val priceUpdateService = mockk<PriceUpdateService> {
        coEvery { withUpdatedPrices(any<OrderVersion>()) } answers { it.invocation.args.first() as OrderVersion }
    }
    private val metrics: ForeignOrderMetrics = mockk {
        every { onDownloadedOrderError(Platform.LOOKSRARE, "incorrect_amount") } returns Unit
    }
    private val currencyAddresses = OrderIndexerProperties.CurrencyContractAddresses(weth = randomAddress())

    private val converter = LooksrareOrderConverter(
        priceUpdateService = priceUpdateService,
        currencyAddresses = currencyAddresses,
        metrics
    )

    @Test
    fun `should convert erc721 sell order with weth currency`() = runBlocking<Unit> {
        val looksrareOrder = randomLooksrareOrder().copy(
            collectionType = CollectionType.ERC721,
            quoteType = QuoteType.ASK,
            amounts = listOf(BigInteger.ONE)
        )

        val orderVersion = converter.convert(looksrareOrder)
        assertThat(orderVersion).isNotNull
        assertThat(orderVersion!!.hash).isEqualTo(looksrareOrder.hash)
        assertThat(orderVersion.maker).isEqualTo(looksrareOrder.signer)
        assertThat(orderVersion.taker).isNull()
        assertThat(orderVersion.make.value.value).isEqualTo(looksrareOrder.amounts.single())
        assertThat(orderVersion.make.type).isInstanceOf(Erc721AssetType::class.java)
        assertThat((orderVersion.make.type as Erc721AssetType).token).isEqualTo(looksrareOrder.collection)
        assertThat((orderVersion.make.type as Erc721AssetType).tokenId.value).isEqualTo(looksrareOrder.itemIds.single())
        assertThat(orderVersion.take.value.value).isEqualTo(looksrareOrder.price)
        assertThat(orderVersion.take.type).isInstanceOf(Erc20AssetType::class.java)
        assertThat((orderVersion.take.type as Erc20AssetType).token).isEqualTo(looksrareOrder.currency)
        assertThat(orderVersion.type).isEqualTo(OrderType.LOOKSRARE_V2)
        assertThat(orderVersion.salt).isEqualTo(EthUInt256.ZERO)
        assertThat(orderVersion.start).isEqualTo(looksrareOrder.startTime.epochSecond)
        assertThat(orderVersion.end).isEqualTo(looksrareOrder.endTime.epochSecond)
        assertThat(orderVersion.createdAt).isEqualTo(looksrareOrder.startTime)
        assertThat(orderVersion.signature).isEqualTo(looksrareOrder.signature)
        assertThat(orderVersion.data).isInstanceOf(OrderLooksrareDataV2::class.java)
        with(orderVersion.data as OrderLooksrareDataV2) {
            assertThat(counterHex.value).isEqualTo(looksrareOrder.globalNonce)
            assertThat(orderNonce.value).isEqualTo(looksrareOrder.orderNonce)
            assertThat(subsetNonce.value).isEqualTo(looksrareOrder.subsetNonce)
            assertThat(strategyId.value).isEqualTo(looksrareOrder.strategyId.toBigInteger())
        }
    }

    @Test
    fun `should convert erc155 sell order with weth currency`() = runBlocking<Unit> {
        val looksrareOrder = randomLooksrareOrder().copy(
            collectionType = CollectionType.ERC1155,
            quoteType = QuoteType.ASK,
            amounts = listOf(BigInteger.TEN)
        )
        val orderVersion = converter.convert(looksrareOrder)
        assertThat(orderVersion!!.make.type).isInstanceOf(Erc1155AssetType::class.java)
        assertThat((orderVersion.make.type as Erc1155AssetType).token).isEqualTo(looksrareOrder.collection)
        assertThat((orderVersion.make.type as Erc1155AssetType).tokenId.value).isEqualTo(looksrareOrder.itemIds.single())
    }

    @Test
    fun `should convert sell order with weth currency`() = runBlocking<Unit> {
        val looksrareOrder = randomLooksrareOrder().copy(
            currency = currencyAddresses.weth,
            collectionType = CollectionType.ERC1155,
            quoteType = QuoteType.ASK,
            amounts = listOf(BigInteger.TEN)
        )
        val orderVersion = converter.convert(looksrareOrder)
        assertThat(orderVersion!!.take.type).isInstanceOf(EthAssetType::class.java)
    }

    @Test
    fun `should convert bid erc721 order`() = runBlocking<Unit> {
        val looksrareOrder = randomLooksrareOrder().copy(
            collectionType = CollectionType.ERC721,
            quoteType = QuoteType.BID,
            amounts = listOf(BigInteger.ONE)
        )
        val orderVersion = converter.convert(looksrareOrder)

        assertThat(orderVersion!!.make.value.value).isEqualTo(looksrareOrder.price)
        assertThat(orderVersion.make.type).isInstanceOf(Erc20AssetType::class.java)
        assertThat((orderVersion.make.type as Erc20AssetType).token).isEqualTo(looksrareOrder.currency)
        assertThat(orderVersion.take.value.value).isEqualTo(looksrareOrder.amounts.single())
        assertThat(orderVersion.take.type).isInstanceOf(Erc721AssetType::class.java)
        assertThat((orderVersion.take.type as Erc721AssetType).token).isEqualTo(looksrareOrder.collection)
        assertThat((orderVersion.take.type as Erc721AssetType).tokenId.value).isEqualTo(looksrareOrder.itemIds.single())
    }
}
