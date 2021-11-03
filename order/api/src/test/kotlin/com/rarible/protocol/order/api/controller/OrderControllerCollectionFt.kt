package com.rarible.protocol.order.api.controller

import com.rarible.core.test.data.randomAddress
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.OrderStatusDto
import com.rarible.protocol.order.api.data.createOrderVersion
import com.rarible.protocol.order.api.integration.AbstractIntegrationTest
import com.rarible.protocol.order.api.integration.IntegrationTest
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.CollectionAssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.OrderVersion
import io.mockk.coEvery
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
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
        coEvery { assetMakeBalanceProvider.getMakeBalance(any()) } returns EthUInt256.TEN
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
            listOf(OrderStatusDto.ACTIVE),
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
    fun `should return sell order by maker`() = runBlocking<Unit> {
        val maker = randomAddress()
        val make = Asset(CollectionAssetType(token), EthUInt256.ONE)
        val take = Asset(EthAssetType, EthUInt256.ONE)
        val orderV1 = createOrderVersion(make, take).copy(maker = maker)
        saveOrderVersions(orderV1)

        val dto = controller.getSellOrdersByMakerAndByStatus(
            maker.hex(), null, null, null, null, listOf(OrderStatusDto.ACTIVE))
        assertEquals(1, dto.body.orders.size)
    }

    private suspend fun saveOrderVersions(vararg order: OrderVersion) {
        order.forEach { orderUpdateService.save(it) }
    }
}
