package com.rarible.protocol.order.listener.service.x2y2

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.model.Erc1155AssetType
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.OrderX2Y2DataV1
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.listener.data.randomX2Y2Order
import com.rarible.protocol.order.core.metric.ForeignOrderMetrics
import com.rarible.x2y2.client.model.ErcType
import com.rarible.x2y2.client.model.Order
import com.rarible.x2y2.client.model.OrderStatus
import com.rarible.x2y2.client.model.OrderType
import com.rarible.x2y2.client.model.Token
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import scalether.domain.Address
import java.math.BigInteger

class X2Y2OrderConverterTest {

    private val metrics = mockk<ForeignOrderMetrics> {
        every { onDownloadedOrderSkipped(Platform.X2Y2, any()) } returns Unit
    }

    private val converter = X2Y2OrderConverter(
        mockk {
            coEvery { withUpdatedPrices(orderVersion = any()) } returnsArgument 0
        },
        metrics
    )

    private companion object {

        @JvmStatic
        fun notOpenStatuses() = OrderStatus.values().filter { it != OrderStatus.OPEN }.stream()
    }

    @Test
    internal fun `should convert erc721 and eth`() {
        runBlocking {
            val nft = Token(
                contract = randomAddress(),
                tokenId = randomBigInt(),
                ercType = ErcType.ERC721
            )
            val currency = Address.ZERO()
            val x2y2Order = randomValidOrder().copy(
                amount = BigInteger.ONE,
                token = nft,
                currency = currency
            )
            val converted = converter.convert(x2y2Order)
            assertThat(converted).isNotNull
            assertThat(converted!!.hash).isEqualTo(x2y2Order.itemHash)
            assertThat(converted.maker).isEqualTo(x2y2Order.maker)
            assertThat(converted.taker).isNull()

            assertThat(converted.make.type).isExactlyInstanceOf(Erc721AssetType::class.java)
            assertThat((converted.make.type as Erc721AssetType).token).isEqualTo(nft.contract)
            assertThat((converted.make.type as Erc721AssetType).tokenId.value).isEqualTo(nft.tokenId)
            assertThat(converted.make.value).isEqualTo(EthUInt256.ONE)

            assertThat(converted.take.type).isExactlyInstanceOf(EthAssetType::class.java)
            assertThat(converted.take.value.value).isEqualTo(x2y2Order.price)

            assertThat(converted.type).isEqualTo(com.rarible.protocol.order.core.model.OrderType.X2Y2)
            assertThat(converted.platform).isEqualTo(Platform.X2Y2)
            assertThat(converted.start).isEqualTo(x2y2Order.createdAt.epochSecond)
            assertThat(converted.end).isEqualTo(x2y2Order.endAt.epochSecond)
            assertThat(converted.createdAt).isEqualTo(x2y2Order.createdAt)
            assertThat(converted.data).isExactlyInstanceOf(OrderX2Y2DataV1::class.java)
            val d = converted.data as OrderX2Y2DataV1
            assertThat(d.itemHash).isEqualTo(x2y2Order.itemHash)
            assertThat(d.isCollectionOffer).isEqualTo(x2y2Order.isCollectionOffer)
            assertThat(d.isBundle).isEqualTo(x2y2Order.isBundle)
            assertThat(d.side).isEqualTo(x2y2Order.side)
            assertThat(d.orderId).isEqualTo(x2y2Order.id)
        }
    }

    @Test
    internal fun `should convert erc1155 and erc20`() {
        runBlocking {
            val nft = Token(
                contract = randomAddress(),
                tokenId = randomBigInt(),
                ercType = ErcType.ERC1155
            )
            val currency = randomAddress()
            val x2y2Order = randomValidOrder().copy(
                amount = randomBigInt(),
                token = nft,
                currency = currency
            )
            val converted = converter.convert(x2y2Order)
            assertThat(converted).isNotNull

            assertThat(converted!!.make.type).isExactlyInstanceOf(Erc1155AssetType::class.java)
            assertThat((converted.make.type as Erc1155AssetType).token).isEqualTo(nft.contract)
            assertThat((converted.make.type as Erc1155AssetType).tokenId.value).isEqualTo(nft.tokenId)
            assertThat(converted.make.value.value).isEqualTo(x2y2Order.amount)

            assertThat(converted.take.type).isExactlyInstanceOf(Erc20AssetType::class.java)
            assertThat((converted.take.type as Erc20AssetType).token).isEqualTo(currency)
            assertThat(converted.take.value.value).isEqualTo(x2y2Order.price)
        }
    }

    @ParameterizedTest
    @MethodSource("notOpenStatuses")
    internal fun `should not convert invalid status orders`(status: OrderStatus) {
        runBlocking {
            val x2y2Order = randomValidOrder().copy(
                status = status
            )
            val converted = converter.convert(x2y2Order)
            assertThat(converted).isNull()
        }
    }

    @Test
    internal fun `should not convert collection orders`() {
        runBlocking {
            val x2y2Order = randomValidOrder().copy(
                isCollectionOffer = true
            )
            val converted = converter.convert(x2y2Order)
            assertThat(converted).isNull()
        }
    }

    @Test
    internal fun `should not convert bundle orders`() {
        runBlocking {
            val x2y2Order = randomValidOrder().copy(
                isBundle = true
            )
            val converted = converter.convert(x2y2Order)
            assertThat(converted).isNull()
        }
    }

    @Test
    internal fun `should not convert buy orders`() {
        runBlocking {
            val x2y2Order = randomValidOrder().copy(
                type = OrderType.BUY,
            )
            val converted = converter.convert(x2y2Order)
            assertThat(converted).isNull()
        }
    }

    private fun randomValidOrder(): Order {
        return randomX2Y2Order().copy(
            status = OrderStatus.OPEN,
            type = OrderType.SELL,
            isBundle = false,
            isCollectionOffer = false
        )
    }
}
