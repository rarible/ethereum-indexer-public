package com.rarible.protocol.order.listener.service.order

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomString
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.Erc20BalanceDto
import com.rarible.protocol.dto.Erc20BalanceUpdateEventDto
import com.rarible.protocol.dto.NftOwnershipDto
import com.rarible.protocol.order.core.data.createOrderOpenSeaV1DataV1
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Erc1155AssetType
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.MakeBalanceState
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.listener.data.createNftOwnershipDeleteEvent
import com.rarible.protocol.order.listener.data.createNftOwnershipDeleteEventLegacy
import com.rarible.protocol.order.listener.data.createNftOwnershipDto
import com.rarible.protocol.order.listener.data.createNftOwnershipUpdateEvent
import com.rarible.protocol.order.listener.data.createOrder
import com.rarible.protocol.order.listener.data.createOrderVersion
import com.rarible.protocol.order.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.order.listener.integration.IntegrationTest
import io.mockk.clearMocks
import io.mockk.coEvery
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.AddressFactory
import java.math.BigInteger

@IntegrationTest
class OrderBalanceServiceTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var orderBalanceService: OrderBalanceService

    @Test
    fun `should update all not canceled balance orders`() = runBlocking<Unit> {
        val targetMaker = AddressFactory.create()
        val targetToken = AddressFactory.create()

        val make = Asset(Erc20AssetType(targetToken), EthUInt256.TEN)
        val take = Asset(Erc1155AssetType(AddressFactory.create(), EthUInt256.TEN), EthUInt256.TEN)

        val oldStock = EthUInt256.of(2)
        val newStock = EthUInt256.of(5)

        clearMocks(assetBalanceProvider)
        coEvery { assetBalanceProvider.getAssetStock(any(), any()) } returns MakeBalanceState(oldStock)

        val order1 = createOrderVersion().copy(
            maker = targetMaker,
            make = make,
            take = take
        )
        val order2 = createOrderVersion().copy(
            maker = targetMaker,
            make = make,
            take = take
        )
        val order3 = createOrderVersion().copy(
            maker = targetMaker,
            make = make,
            take = take
        )
        val order4 = createOrderVersion().copy(
            maker = AddressFactory.create(),
            make = make,
            take = take
        )

        listOf(order1, order2, order3, order4).forEach { orderUpdateService.save(it) }
        cancelOrder(order3.hash)

        val updatedBalance = Erc20BalanceDto(
            owner = targetMaker,
            contract = targetToken,
            balance = newStock.value
        )
        val event = Erc20BalanceUpdateEventDto(
            eventId = randomString(),
            balanceId = randomString(),
            balance = updatedBalance
        )

        // Background job might update makeStock before this event is handled => try until the event solely changes the makeStock.
        Wait.waitAssert {
            orderBalanceService.handle(event)

            assertThat(orderRepository.findById(order1.hash)?.makeStock).isEqualTo(newStock)
            assertThat(orderRepository.findById(order2.hash)?.makeStock).isEqualTo(newStock)
            assertThat(orderRepository.findById(order3.hash)?.makeStock).isEqualTo(EthUInt256.ZERO) // because order #3 is a cancelled order.
            assertThat(orderRepository.findById(order4.hash)?.makeStock).isEqualTo(oldStock)
        }
    }

    @Test
    fun `should not update legacy OpenSea orders`() = runBlocking<Unit> {
        val legacyOpenSea = randomAddress()
        orderIndexerProperties.exchangeContractAddresses.openSeaV1 = legacyOpenSea

        val targetMaker = AddressFactory.create()
        val targetToken = AddressFactory.create()

        val make = Asset(Erc20AssetType(targetToken), EthUInt256.TEN)
        val take = Asset(Erc1155AssetType(AddressFactory.create(), EthUInt256.TEN), EthUInt256.TEN)

        val oldStock = EthUInt256.of(2)
        val newStock = EthUInt256.of(5)

        clearMocks(assetBalanceProvider)
        coEvery { assetBalanceProvider.getAssetStock(any(), any()) } returns MakeBalanceState(oldStock)

        val order1 = createOrderVersion().copy(
            maker = targetMaker,
            make = make,
            take = take
        )
        val order2 = createOrder().copy(
            maker = targetMaker,
            make = make,
            take = take,
            type = OrderType.OPEN_SEA_V1,
            data = createOrderOpenSeaV1DataV1().copy(exchange = legacyOpenSea),
            makeStock = oldStock
        )
        orderUpdateService.save(order1)
        orderRepository.save(order2)

        val updatedBalance = Erc20BalanceDto(
            owner = targetMaker,
            contract = targetToken,
            balance = newStock.value
        )
        val event = Erc20BalanceUpdateEventDto(
            eventId = randomString(),
            balanceId = randomString(),
            balance = updatedBalance
        )

        // Background job might update makeStock before this event is handled => try until the event solely changes the makeStock.
        Wait.waitAssert {
            orderBalanceService.handle(event)

            assertThat(orderRepository.findById(order1.hash)?.makeStock).isEqualTo(newStock)
            assertThat(orderRepository.findById(order2.hash)?.makeStock).isEqualTo(oldStock)
        }
    }

    @Test
    fun `should update all not canceled nft orders`() = runBlocking<Unit> {
        val ownership = createNftOwnershipDto()
            .copy(tokenId = BigInteger.valueOf(2))
            .copy(value = BigInteger.valueOf(5))

        val targetToken = ownership.contract
        val targetTokenId = EthUInt256.of(ownership.tokenId)

        val make = Asset(Erc1155AssetType(targetToken, targetTokenId), EthUInt256.TEN)
        val take = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.TEN)
        val maker = ownership.owner

        val order1 = createOrderVersion().copy(
            maker = maker,
            make = make,
            take = take
        )
        val order2 = createOrderVersion().copy(
            maker = maker,
            make = make,
            take = take
        )
        val order3 = createOrderVersion().copy(
            maker = maker,
            make = make,
            take = take
            // cancelled
        )
        val order4 = createOrderVersion().copy(
            make = make
            // other maker => different hash
        )

        val oldStock = EthUInt256.ONE

        clearMocks(assetBalanceProvider)
        coEvery { assetBalanceProvider.getAssetStock(any(), any()) } returns MakeBalanceState(oldStock)

        listOf(order1, order2, order3, order4).forEach { orderUpdateService.save(it) }
        cancelOrder(order3.hash)

        val event = createNftOwnershipUpdateEvent(ownership)

        Wait.waitAssert {
            orderBalanceService.handle(event)

            assertThat(orderRepository.findById(order1.hash)?.makeStock?.value).isEqualTo(ownership.value)
            assertThat(orderRepository.findById(order2.hash)?.makeStock?.value).isEqualTo(ownership.value)
            assertThat(orderRepository.findById(order3.hash)?.makeStock).isEqualTo(EthUInt256.ZERO) // because order #3 is a cancelled order.
            assertThat(orderRepository.findById(order4.hash)?.makeStock).isEqualTo(oldStock)
        }
    }

    @Test
    fun `should not update legacy OpenSea sell orders`() = runBlocking<Unit> {
        val legacyOpenSea = randomAddress()
        orderIndexerProperties.exchangeContractAddresses.openSeaV1 = legacyOpenSea

        val ownership = createNftOwnershipDto()
            .copy(tokenId = BigInteger.valueOf(2))
            .copy(value = BigInteger.valueOf(5))

        val targetToken = ownership.contract
        val targetTokenId = EthUInt256.of(ownership.tokenId)

        val make = Asset(Erc1155AssetType(targetToken, targetTokenId), EthUInt256.TEN)
        val take = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.TEN)
        val maker = ownership.owner

        val oldStock = EthUInt256.ONE

        clearMocks(assetBalanceProvider)
        coEvery { assetBalanceProvider.getAssetStock(any(), any()) } returns MakeBalanceState(oldStock)

        val order1 = createOrderVersion().copy(
            maker = maker,
            make = make,
            take = take
        )
        val order2 = createOrder().copy(
            maker = maker,
            make = make,
            take = take,
            type = OrderType.OPEN_SEA_V1,
            data = createOrderOpenSeaV1DataV1().copy(exchange = legacyOpenSea),
            makeStock = oldStock
        )
        orderUpdateService.save(order1)
        orderRepository.save(order2)

        val event = createNftOwnershipUpdateEvent(ownership)

        Wait.waitAssert {
            orderBalanceService.handle(event)

            assertThat(orderRepository.findById(order1.hash)?.makeStock?.value).isEqualTo(ownership.value)
            assertThat(orderRepository.findById(order2.hash)?.makeStock?.value).isEqualTo(oldStock.value)
        }
    }

    @Test
    fun `on ownership updated - lastUpdate of order updated`() = runBlocking<Unit> {
        val now = nowMillis()
        val ownership = createNftOwnershipDto().copy(
            tokenId = BigInteger.valueOf(2),
            value = BigInteger.valueOf(7),
            date = now
        )

        val orderVersion = createOrderVersionForOwnership(ownership, 10, 10)
            .copy(createdAt = now.minusSeconds(10))

        val order = orderUpdateService.save(orderVersion)

        val event = createNftOwnershipUpdateEvent(ownership)

        orderBalanceService.handle(event)

        val updatedOrder = orderRepository.findById(order.hash)!!
        assertThat(updatedOrder.makeStock.value).isEqualTo(ownership.value)
        assertThat(updatedOrder.lastUpdateAt).isEqualTo(ownership.date)
    }

    @Test
    fun `on ownership updated - lastUpdate of order not updated`() = runBlocking<Unit> {
        val now = nowMillis()
        val ownership = createNftOwnershipDto().copy(
            tokenId = BigInteger.valueOf(3),
            value = BigInteger.valueOf(7),
            date = now.minusSeconds(120)
        )

        val orderVersion = createOrderVersionForOwnership(ownership, 10, 10)
        val order = orderUpdateService.save(orderVersion)

        val event = createNftOwnershipUpdateEvent(ownership)

        orderBalanceService.handle(event)

        val updatedOrder = orderRepository.findById(order.hash)!!
        assertThat(updatedOrder.makeStock.value).isEqualTo(ownership.value)
        assertThat(updatedOrder.lastUpdateAt).isEqualTo(order.lastUpdateAt)
    }

    @Test
    fun `on ownership delete event - legacy`() = runBlocking<Unit> {
        val now = nowMillis()
        val ownership = createNftOwnershipDto().copy(
            tokenId = BigInteger.valueOf(2),
            value = BigInteger.ZERO,
            date = now
        )

        val initialStock = EthUInt256.ONE
        // lastUpdate of order should be replaced by latest date of asset update
        val assetStock = MakeBalanceState(initialStock, now.minusSeconds(60))

        clearMocks(assetBalanceProvider)
        coEvery { assetBalanceProvider.getAssetStock(any(), any()) } returns assetStock

        val orderVersion = createOrderVersionForOwnership(ownership, 1, 10)
            .copy(createdAt = now.minusSeconds(120))
        val order = orderUpdateService.save(orderVersion)

        assertThat(orderRepository.findById(order.hash)?.makeStock).isEqualTo(initialStock)

        val event = createNftOwnershipDeleteEventLegacy(ownership)

        orderBalanceService.handle(event)

        val updatedOrder = orderRepository.findById(order.hash)!!
        assertThat(updatedOrder.makeStock).isEqualTo(EthUInt256.ZERO)
        assertThat(updatedOrder.lastUpdateAt).isEqualTo(assetStock.lastUpdatedAt)
    }

    @Test
    fun `on ownership deleted - full event`() = runBlocking<Unit> {
        val now = nowMillis()
        val ownership = createNftOwnershipDto().copy(
            tokenId = BigInteger.valueOf(2),
            value = BigInteger.ZERO,
            date = now
        )

        val orderVersion = createOrderVersionForOwnership(ownership, 1, 10)
            .copy(createdAt = now.minusSeconds(1))

        val order = orderUpdateService.save(orderVersion)

        val event = createNftOwnershipDeleteEvent(ownership)

        orderBalanceService.handle(event)

        val updatedOrder = orderRepository.findById(order.hash)!!
        assertThat(updatedOrder.makeStock.value).isEqualTo(ownership.value)
        assertThat(updatedOrder.lastUpdateAt).isEqualTo(ownership.date)
    }

    private fun createOrderVersionForOwnership(
        ownership: NftOwnershipDto,
        makeValue: Int,
        takeValue: Int
    ): OrderVersion {
        val targetToken = ownership.contract
        val targetTokenId = EthUInt256.of(ownership.tokenId)

        val make = Asset(Erc721AssetType(targetToken, targetTokenId), EthUInt256.of(makeValue))
        val take = Asset(EthAssetType, EthUInt256.of(takeValue))

        return createOrderVersion().copy(
            maker = ownership.owner,
            make = make,
            take = take,
            createdAt = nowMillis()
        )
    }
}
