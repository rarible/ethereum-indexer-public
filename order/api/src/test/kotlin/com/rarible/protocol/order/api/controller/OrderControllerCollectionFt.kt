package com.rarible.protocol.order.api.controller

import com.rarible.core.test.data.randomAddress
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.OrderIdsDto
import com.rarible.protocol.dto.OrderStatusDto
import com.rarible.protocol.order.api.data.createOrderVersion
import com.rarible.protocol.order.api.integration.AbstractIntegrationTest
import com.rarible.protocol.order.api.integration.IntegrationTest
import com.rarible.protocol.order.core.data.randomOrder
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.CollectionAssetType
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.GenerativeArtAssetType
import com.rarible.protocol.order.core.model.MakeBalanceState
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderVersion
import io.mockk.coEvery
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.Address
import java.math.BigInteger

@IntegrationTest
class OrderControllerCollectionFt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var controller: OrderController

    private lateinit var token: Address
    private lateinit var tokenId: BigInteger

    @BeforeEach
    fun mockMakeBalance() {
        // Make all orders have status ACTIVE
        coEvery { assetMakeBalanceProvider.getMakeBalance(any()) } returns MakeBalanceState(EthUInt256.TEN)
    }

    @BeforeEach
    fun initializeToken() {
        token = randomAddress()
        tokenId = BigInteger.valueOf(2)
    }

    @Test
    fun `should return bid order by item`() = runBlocking<Unit> {
        val maker = randomAddress()
        val make = Asset(EthAssetType, EthUInt256.ONE)
        val take = Asset(CollectionAssetType(token), EthUInt256.ONE)
        val orderV1 = createOrderVersion(make, take).copy(maker = maker)
        saveOrderVersions(orderV1)

        val dto = controller.getOrderBidsByItemAndByStatus(
            token.hex(),
            tokenId.toString(),
            null,
            null,
            null,
            null,
            null,
            listOf(OrderStatusDto.ACTIVE),
            null,
            null,
            null
        )
        assertEquals(1, dto.body.orders.size)
    }

    @Test
    fun `should return bid order by item - without status filter`() = runBlocking<Unit> {
        val maker = randomAddress()
        val make = Asset(EthAssetType, EthUInt256.ONE)
        val take = Asset(CollectionAssetType(token), EthUInt256.ONE)
        val orderV1 = createOrderVersion(make, take).copy(maker = maker)
        saveOrderVersions(orderV1)

        val dto = controller.getOrderBidsByItemAndByStatus(
            token.hex(),
            tokenId.toString(),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        )
        assertEquals(1, dto.body.orders.size)
    }

    @Test
    @Disabled("We don't have collection sell orders ATM") // TODO PT-1652
    fun `should return sell order by item`() = runBlocking<Unit> {
        val maker = randomAddress()
        val make = Asset(CollectionAssetType(token), EthUInt256.ONE)
        val take = Asset(EthAssetType, EthUInt256.ONE)
        val orderV1 = createOrderVersion(make, take).copy(maker = maker)
        saveOrderVersions(orderV1)

        val dto = controller.getSellOrdersByItemAndByStatus(
            token.hex(), tokenId.toString(), null, null, null, null, null, listOf(OrderStatusDto.ACTIVE),
            null
        )
        assertEquals(1, dto.body.orders.size)
    }

    @Test
    fun `shouldn't return sell order by item`() = runBlocking<Unit> {
        val maker = randomAddress()
        val make1 = Asset(GenerativeArtAssetType(token), EthUInt256.ONE)
        val take1 = Asset(EthAssetType, EthUInt256.ONE)
        val orderV1 = createOrderVersion(make1, take1).copy(maker = maker)
        saveOrderVersions(orderV1)

        val make2 = Asset(Erc721AssetType(token, EthUInt256.of(tokenId)), EthUInt256.ONE)
        val take2 = Asset(EthAssetType, EthUInt256.ONE)
        val orderV2 = createOrderVersion(make2, take2).copy(maker = maker)
        saveOrderVersions(orderV2)

        val dto = controller.getSellOrdersByItemAndByStatus(
            token.hex(), tokenId.toString(), null, null, null, null, null, listOf(OrderStatusDto.ACTIVE),
            null
        )
        assertEquals(1, dto.body.orders.size)
    }

    @Test
    fun `should return sell order by maker`() = runBlocking<Unit> {
        val maker = randomAddress()
        val maker2 = randomAddress()
        val maker3 = randomAddress()
        val make = Asset(CollectionAssetType(token), EthUInt256.ONE)
        val take = Asset(EthAssetType, EthUInt256.ONE)
        val orderV1 = createOrderVersion(make, take).copy(maker = maker)
        val orderV2 = createOrderVersion(make, take).copy(maker = maker2)
        val orderV3 = createOrderVersion(make, take).copy(maker = maker3)
        saveOrderVersions(orderV1, orderV2, orderV3)

        val dto = controller.getSellOrdersByMakerAndByStatus(
            listOf(maker, maker2), null, null, null, null, listOf(OrderStatusDto.ACTIVE))
        assertEquals(2, dto.body.orders.size)
        assertThat(dto.body.orders.map { it.maker }).containsExactlyInAnyOrder(maker, maker2)
    }

    @Test
    fun `should return orders by id`() = runBlocking<Unit> {
        val order1 = randomOrder()
        val order2 = randomOrder()
        saveOrders(order1, order2)
        val request = OrderIdsDto(ids = listOf(order1.hash.prefixed(), order2.hash.prefixed()))

        val dto = controller.getByIds(request).body?.orders
        assertThat(dto?.map { it.id }).containsExactlyInAnyOrderElementsOf(request.ids)
    }

    private suspend fun saveOrderVersions(vararg order: OrderVersion) {
        order.forEach { save(it) }
    }

    private suspend fun saveOrders(vararg order: Order) {
        order.forEach { orderRepository.save(it) }
    }
}
