package com.rarible.protocol.order.listener.service.order

import com.rarible.core.test.data.randomString
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.Erc20BalanceDto
import com.rarible.protocol.dto.Erc20BalanceUpdateEventDto
import com.rarible.protocol.dto.NftDeletedOwnershipDto
import com.rarible.protocol.dto.NftOwnershipDeleteEventDto
import com.rarible.protocol.dto.NftOwnershipUpdateEventDto
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Erc1155AssetType
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.MakeBalanceState
import com.rarible.protocol.order.listener.data.createNftOwnershipDto
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
    fun `should update all not canceled nft orders`() = runBlocking<Unit> {
        val ownershipDto = createNftOwnershipDto()
            .copy(tokenId = BigInteger.valueOf(2))
            .copy(value = BigInteger.valueOf(5))

        val targetToken = ownershipDto.contract
        val targetTokenId = EthUInt256.of(ownershipDto.tokenId)

        val make = Asset(Erc1155AssetType(targetToken, targetTokenId), EthUInt256.TEN)
        val take = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.TEN)
        val maker = ownershipDto.owner

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

        val event = NftOwnershipUpdateEventDto(
            eventId = randomString(),
            ownershipId = ownershipDto.id,
            ownership = ownershipDto
        )

        Wait.waitAssert {
            orderBalanceService.handle(event)

            assertThat(orderRepository.findById(order1.hash)?.makeStock?.value).isEqualTo(ownershipDto.value)
            assertThat(orderRepository.findById(order2.hash)?.makeStock?.value).isEqualTo(ownershipDto.value)
            assertThat(orderRepository.findById(order3.hash)?.makeStock).isEqualTo(EthUInt256.ZERO) // because order #3 is a cancelled order.
            assertThat(orderRepository.findById(order4.hash)?.makeStock).isEqualTo(oldStock)
        }
    }

    @Test
    fun `sell order makeStock becomes 0 when make NFT is transferred`() = runBlocking<Unit> {
        val ownershipDto = createNftOwnershipDto()
            .copy(tokenId = BigInteger.valueOf(2))
            .copy(value = BigInteger.ZERO)

        val targetToken = ownershipDto.contract
        val targetTokenId = EthUInt256.of(ownershipDto.tokenId)

        val make = Asset(Erc721AssetType(targetToken, targetTokenId), EthUInt256.ONE)
        val take = Asset(EthAssetType, EthUInt256.TEN)
        val oldOwner = ownershipDto.owner

        val initialStock = EthUInt256.ONE
        clearMocks(assetBalanceProvider)
        coEvery { assetBalanceProvider.getAssetStock(any(), any()) } returns MakeBalanceState(initialStock)
        val order = orderUpdateService.save(
            createOrderVersion().copy(
                maker = oldOwner,
                make = make,
                take = take
            )
        )
        assertThat(orderRepository.findById(order.hash)?.makeStock).isEqualTo(initialStock)

        val deletedOwnership = NftDeletedOwnershipDto(
            id = randomString(),
            token = targetToken,
            tokenId = targetTokenId.value,
            owner = oldOwner
        )

        val event = NftOwnershipDeleteEventDto(
            eventId = randomString(),
            ownershipId = ownershipDto.id,
            ownership = deletedOwnership,
            deletedOwnership = null // Legacy event structure
        )

        orderBalanceService.handle(event)
        assertThat(orderRepository.findById(order.hash)?.makeStock).isEqualTo(EthUInt256.ZERO)
    }
}
