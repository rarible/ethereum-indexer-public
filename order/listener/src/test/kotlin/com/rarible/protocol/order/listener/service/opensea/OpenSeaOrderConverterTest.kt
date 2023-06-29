package com.rarible.protocol.order.listener.service.opensea

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomWord
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.opensea.client.model.v2.ItemType
import com.rarible.opensea.client.model.v2.OrderType
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Erc1155AssetType
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.OrderBasicSeaportDataV1
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.model.SeaportItemType
import com.rarible.protocol.order.core.model.SeaportOrderType
import com.rarible.protocol.order.core.service.PriceUpdateService
import com.rarible.protocol.order.listener.configuration.OrderListenerProperties
import com.rarible.protocol.order.listener.configuration.SeaportLoadProperties
import com.rarible.protocol.order.listener.data.randomConsideration
import com.rarible.protocol.order.listener.data.randomOffer
import com.rarible.protocol.order.listener.data.randomOrderParameters
import com.rarible.protocol.order.listener.data.randomProtocolData
import com.rarible.protocol.order.listener.data.randomSeaportOrder
import com.rarible.protocol.order.listener.misc.ForeignOrderMetrics
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigInteger

internal class OpenSeaOrderConverterTest {

    private val featureFlags = mockk<OrderIndexerProperties.FeatureFlags>()
    private val exchangeContracts = mockk<OrderIndexerProperties.ExchangeContractAddresses>()
    private val metrics = mockk<ForeignOrderMetrics> {
        every { onDownloadedOrderSkipped(any(), any()) } returns Unit
        every { onDownloadedOrderError(any(), any()) } returns Unit
    }
    private val seaportLoadProperties = mockk<SeaportLoadProperties>() {
        every { ignoredSellTokens } returns emptyList()
    }
    private val properties = mockk<OrderListenerProperties> {
        every { openSeaExchangeDomainHashV2 } returns randomWord()
        every { seaportLoad } returns seaportLoadProperties
    }
    private val priceUpdateService = mockk<PriceUpdateService> {
        coEvery { withUpdatedPrices(any<OrderVersion>()) } coAnswers { it.invocation.args.first() as OrderVersion }
    }
    private val converter = OpenSeaOrderConverter(
        priceUpdateService = priceUpdateService,
        exchangeContracts = exchangeContracts,
        featureFlags = featureFlags,
        properties = properties,
        metrics = metrics
    )

    @Test
    fun `should convert seaport order type`() {
        assertThat(converter.convert(OrderType.FULL_OPEN)).isEqualTo(SeaportOrderType.FULL_OPEN)
        assertThat(converter.convert(OrderType.FULL_RESTRICTED)).isEqualTo(SeaportOrderType.FULL_RESTRICTED)
        assertThat(converter.convert(OrderType.PARTIAL_OPEN)).isEqualTo(SeaportOrderType.PARTIAL_OPEN)
        assertThat(converter.convert(OrderType.PARTIAL_RESTRICTED)).isEqualTo(SeaportOrderType.PARTIAL_RESTRICTED)
        assertThat(OrderType.values()).hasSize(4)
        assertThat(SeaportOrderType.values()).hasSize(5)
    }

    @Test
    fun `should convert seaport item type`() {
        assertThat(converter.convert(ItemType.NATIVE)).isEqualTo(SeaportItemType.NATIVE)
        assertThat(converter.convert(ItemType.ERC20)).isEqualTo(SeaportItemType.ERC20)
        assertThat(converter.convert(ItemType.ERC721)).isEqualTo(SeaportItemType.ERC721)
        assertThat(converter.convert(ItemType.ERC1155)).isEqualTo(SeaportItemType.ERC1155)
        assertThat(converter.convert(ItemType.ERC721_WITH_CRITERIA)).isEqualTo(SeaportItemType.ERC721_WITH_CRITERIA)
        assertThat(converter.convert(ItemType.ERC1155_WITH_CRITERIA)).isEqualTo(SeaportItemType.ERC1155_WITH_CRITERIA)
        assertThat(ItemType.values()).hasSize(6)
        assertThat(SeaportItemType.values()).hasSize(6)
    }

    @Test
    fun `should convert seaport offer`() {
        val offer = randomOffer().copy(itemType = ItemType.ERC721)
        val seaportOffer = converter.convert(offer)
        assertThat(seaportOffer.token).isEqualTo(offer.token)
        assertThat(seaportOffer.itemType).isEqualTo(SeaportItemType.ERC721)
        assertThat(seaportOffer.identifier).isEqualTo(offer.identifierOrCriteria)
        assertThat(seaportOffer.startAmount).isEqualTo(offer.startAmount)
        assertThat(seaportOffer.endAmount).isEqualTo(offer.endAmount)
    }

    @Test
    fun `should convert seaport consideration`() {
        val consideration = randomConsideration().copy(itemType = ItemType.ERC1155)
        val seaportConsideration = converter.convert(consideration)
        assertThat(seaportConsideration.token).isEqualTo(consideration.token)
        assertThat(seaportConsideration.itemType).isEqualTo(SeaportItemType.ERC1155)
        assertThat(seaportConsideration.identifier).isEqualTo(consideration.identifierOrCriteria)
        assertThat(seaportConsideration.startAmount).isEqualTo(consideration.startAmount)
        assertThat(seaportConsideration.endAmount).isEqualTo(consideration.endAmount)
        assertThat(seaportConsideration.recipient).isEqualTo(consideration.recipient)
    }

    @Test
    fun `convert seaport erc20 item to asser type`() {
        val item = randomOffer().copy(itemType = ItemType.ERC20)
        val assertType = converter.convertToAssetType(item)
        assertThat(assertType).isEqualTo(Erc20AssetType(item.token))
    }

    @Test
    fun `convert seaport native item to asser type`() {
        val item = randomOffer().copy(itemType = ItemType.NATIVE)
        val assertType = converter.convertToAssetType(item)
        assertThat(assertType).isEqualTo(EthAssetType)
    }

    @Test
    fun `convert seaport erc721 item to asser type`() {
        val item = randomOffer().copy(itemType = ItemType.ERC721)
        val assertType = converter.convertToAssetType(item)
        assertThat(assertType).isEqualTo(Erc721AssetType(item.token, EthUInt256.of(item.identifierOrCriteria)))
    }

    @Test
    fun `convert seaport erc1155 item to asser type`() {
        val item = randomOffer().copy(itemType = ItemType.ERC1155)
        val assertType = converter.convertToAssetType(item)
        assertThat(assertType).isEqualTo(Erc1155AssetType(item.token, EthUInt256.of(item.identifierOrCriteria)))
    }

    @Test
    fun `convert seaport item to asset`() {
        val amount = randomBigInt()
        val item = randomOffer().copy(itemType = ItemType.ERC1155, startAmount = amount, endAmount = amount)
        val assert = converter.convertToAsset(item)
        val expectedAsset = Asset(
            Erc1155AssetType(item.token, EthUInt256.of(item.identifierOrCriteria)), EthUInt256.of(amount)
        )
        assertThat(assert).isEqualTo(expectedAsset)
    }

    @Test
    fun `convert seaport items to asset`() {
        val amount1 = randomBigInt()
        val amount2 = randomBigInt()
        val amount3 = randomBigInt()

        val item1 = randomOffer().copy(itemType = ItemType.NATIVE, startAmount = amount1, endAmount = amount1)
        val item2 = randomOffer().copy(itemType = ItemType.NATIVE, startAmount = amount2, endAmount = amount2)
        val item3 = randomOffer().copy(itemType = ItemType.NATIVE, startAmount = amount3, endAmount = amount3)
        val assert = converter.convertToAsset(listOf(item1, item2, item3))
        val expectedAsset = Asset(
            EthAssetType, EthUInt256.of(amount1) + EthUInt256.of(amount2) + EthUInt256.of(amount3)
        )
        assertThat(assert).isEqualTo(expectedAsset)
    }

    @Test
    fun `convert seaport order`() = runBlocking<Unit> {
        val offer = randomOffer().copy(
            itemType = ItemType.ERC721, startAmount = BigInteger.ONE, endAmount = BigInteger.ONE
        )

        val paymentToken = randomAddress()
        val amount1 = randomBigInt()
        val consideration1 = randomConsideration().copy(
            itemType = ItemType.ERC20, token = paymentToken, startAmount = amount1, endAmount = amount1
        )

        val amount2 = randomBigInt()
        val consideration2 = randomConsideration().copy(
            itemType = ItemType.ERC20, token = paymentToken, startAmount = amount2, endAmount = amount2
        )

        val parameters = randomOrderParameters().copy(
            offer = listOf(offer), consideration = listOf(consideration1, consideration2),
            orderType = OrderType.PARTIAL_RESTRICTED
        )
        val protocolData = randomProtocolData().copy(parameters = parameters)

        val seaportOrder = randomSeaportOrder()
            .copy(
                taker = null,
                protocolData = protocolData,
                currentPrice = amount1 + amount2,
                orderType = com.rarible.opensea.client.model.v2.SeaportOrderType.BASIC
            )
        val orderVersion = converter.convert(seaportOrder)
        assertThat(orderVersion!!.hash).isEqualTo(seaportOrder.orderHash)
        assertThat(orderVersion.maker).isEqualTo(parameters.offerer)
        assertThat(orderVersion.make).isEqualTo(
            Asset(Erc721AssetType(offer.token, EthUInt256.of(offer.identifierOrCriteria)), EthUInt256.ONE)
        )
        assertThat(orderVersion.take).isEqualTo(Asset(Erc20AssetType(paymentToken), EthUInt256.of(amount1 + amount2)))
        assertThat(orderVersion.type).isEqualTo(com.rarible.protocol.order.core.model.OrderType.SEAPORT_V1)
        assertThat(orderVersion.salt.value).isEqualTo(parameters.salt)
        assertThat(orderVersion.start).isEqualTo(parameters.startTime.toLong())
        assertThat(orderVersion.end).isEqualTo(parameters.endTime.toLong())
        assertThat(orderVersion.createdAt).isEqualTo(seaportOrder.createdAt)
        assertThat(orderVersion.signature).isEqualTo(protocolData.signature)
        assertThat(orderVersion.platform).isEqualTo(Platform.OPEN_SEA)
        assertThat(orderVersion.data).isInstanceOf(OrderBasicSeaportDataV1::class.java)
        with(orderVersion.data as OrderBasicSeaportDataV1) {
            assertThat(this.protocol).isEqualTo(seaportOrder.protocolAddress)
            assertThat(this.orderType).isEqualTo(SeaportOrderType.PARTIAL_RESTRICTED)
            assertThat(this.zone).isEqualTo(parameters.zone)
            assertThat(this.zoneHash).isEqualTo(parameters.zoneHash)
            assertThat(this.conduitKey).isEqualTo(parameters.conduitKey)
            assertThat(this.counter).isEqualTo(parameters.counter.toLong())
            assertThat(this.counterHex!!.value).isEqualTo(parameters.counter)
            assertThat(this.offer.single().token).isEqualTo(offer.token)
            assertThat(this.consideration[0].token).isEqualTo(consideration1.token)
            assertThat(this.consideration[1].token).isEqualTo(consideration1.token)
        }
    }

    @Test
    fun `should ignore order with specific token`() = runBlocking<Unit> {
        val offer = randomOffer().copy(
            itemType = ItemType.ERC721, startAmount = BigInteger.ONE, endAmount = BigInteger.ONE
        )
        val paymentToken = randomAddress()
        val amount1 = randomBigInt()
        val consideration1 = randomConsideration().copy(
            itemType = ItemType.ERC20, token = paymentToken, startAmount = amount1, endAmount = amount1
        )
        val amount2 = randomBigInt()
        val consideration2 = randomConsideration().copy(
            itemType = ItemType.ERC20, token = paymentToken, startAmount = amount2, endAmount = amount2
        )
        val parameters = randomOrderParameters().copy(
            offer = listOf(offer), consideration = listOf(consideration1, consideration2),
            orderType = OrderType.PARTIAL_RESTRICTED
        )
        val protocolData = randomProtocolData().copy(parameters = parameters)

        val seaportOrder = randomSeaportOrder()
            .copy(
                taker = null,
                protocolData = protocolData,
                currentPrice = amount1 + amount2,
                orderType = com.rarible.opensea.client.model.v2.SeaportOrderType.BASIC
            )
        every { seaportLoadProperties.ignoredSellTokens } returns listOf(offer.token)

        val orderVersion = converter.convert(seaportOrder)
        assertThat(orderVersion).isNull()
    }

    @Test
    fun `convert seaport order - total amount != current price`() = runBlocking<Unit> {
        val consideration = randomConsideration().copy(itemType = ItemType.ERC20)
        val offer = randomOffer().copy(itemType = ItemType.ERC721)

        val seaportOrder = randomSeaportOrder()
            .copy(
                taker = null,
                protocolData = randomProtocolData().copy(
                    parameters = randomOrderParameters().copy(
                        consideration = listOf(consideration),
                        offer = listOf(offer)
                    )
                ),
                currentPrice = randomBigInt(),
                orderType = com.rarible.opensea.client.model.v2.SeaportOrderType.BASIC
            )

        val orderVersion = converter.convert(seaportOrder)

        assertThat(orderVersion).isNull()
    }
}