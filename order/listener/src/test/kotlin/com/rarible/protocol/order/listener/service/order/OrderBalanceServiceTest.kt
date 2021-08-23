package com.rarible.protocol.order.listener.service.order

import com.rarible.core.contract.model.Erc20Token
import com.rarible.ethereum.contract.service.ContractService
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.Erc20BalanceDto
import com.rarible.protocol.dto.Erc20BalanceUpdateEventDto
import com.rarible.protocol.dto.NftOwnershipDto
import com.rarible.protocol.dto.NftOwnershipUpdateEventDto
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Erc1155AssetType
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.listener.data.createOrder
import com.rarible.protocol.order.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.order.listener.integration.IntegrationTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import scalether.domain.Address
import scalether.domain.AddressFactory

@IntegrationTest
@FlowPreview
@Import(OrderBalanceServiceTest.TestContractService::class)
internal class OrderBalanceServiceTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var orderBalanceService: OrderBalanceService


    @Test
    fun `should update all not canceled balance orders`() = runBlocking<Unit> {
        val targetMaker = AddressFactory.create()
        val targetToken = AddressFactory.create()
        val stock = EthUInt256.of(5)
        val make = Asset(Erc20AssetType(targetToken), EthUInt256.TEN)
        val take = Asset(Erc1155AssetType(AddressFactory.create(), EthUInt256.TEN), EthUInt256.TEN)

        val order1 = createOrder().copy(
            maker = targetMaker,
            make = make,
            take = take,
            cancelled = false
        )
        val order2 = createOrder().copy(
            maker = targetMaker,
            make = make,
            take = take,
            cancelled = false
        )
        val order3 = createOrder().copy(
            maker = targetMaker,
            make = make,
            take = take,
            makeStock = EthUInt256.ONE,
            cancelled = true
        )
        val order4 = createOrder().copy(
            maker = AddressFactory.create(),
            make = make,
            take = take,
            makeStock = EthUInt256.ONE,
            cancelled = false
        )
        listOf(order1, order2, order3, order4).forEach { orderRepository.save(it) }

        val updatedBalance = mockk<Erc20BalanceDto> {
            every { owner } returns targetMaker
            every { contract } returns targetToken
            every { balance } returns stock.value
        }
        val event = mockk<Erc20BalanceUpdateEventDto> {
            every { balance } returns updatedBalance
        }

        orderBalanceService.handle(event)

        assertThat(orderRepository.findById(order1.hash)?.makeStock).isEqualTo(stock)
        assertThat(orderRepository.findById(order2.hash)?.makeStock).isEqualTo(stock)
        assertThat(orderRepository.findById(order3.hash)?.makeStock).isEqualTo(EthUInt256.ONE)
        assertThat(orderRepository.findById(order4.hash)?.makeStock).isEqualTo(EthUInt256.ONE)
    }

    @Test
    fun `should update all not canceled nft orders`() = runBlocking<Unit> {
        val targetToken = AddressFactory.create()
        val targetTokenId = EthUInt256.of(2)
        val stock = EthUInt256.of(5)
        val make = Asset(Erc1155AssetType(targetToken, targetTokenId), EthUInt256.TEN)
        val take = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.TEN)
        val maker = AddressFactory.create()

        val order1 = createOrder().copy(
            maker = maker,
            make = make,
            take = take,
            cancelled = false
        )
        val order2 = createOrder().copy(
            maker = maker,
            make = make,
            take = take,
            cancelled = false
        )
        val order3 = createOrder().copy(
            maker = maker,
            make = make,
            take = take,
            makeStock = EthUInt256.ONE,
            cancelled = true
        )
        val order4 = createOrder().copy(
            make = make,
            makeStock = EthUInt256.ONE,
            cancelled = false
        )
        listOf(order1, order2, order3, order4).forEach { orderRepository.save(it) }

        val updatedOwnership = mockk<NftOwnershipDto> {
            every { owner } returns maker
            every { contract } returns targetToken
            every { tokenId } returns targetTokenId.value
            every { value } returns stock.value
        }
        val event = mockk<NftOwnershipUpdateEventDto> {
            every { ownership } returns updatedOwnership
        }

        orderBalanceService.handle(event)

        assertThat(orderRepository.findById(order1.hash)?.makeStock).isEqualTo(stock)
        assertThat(orderRepository.findById(order2.hash)?.makeStock).isEqualTo(stock)
        assertThat(orderRepository.findById(order3.hash)?.makeStock).isEqualTo(EthUInt256.ONE)
        assertThat(orderRepository.findById(order4.hash)?.makeStock).isEqualTo(EthUInt256.ONE)
    }

    internal class TestContractService {
        @Bean
        fun mockkContractService(): ContractService {
            val service = mockk<ContractService>()
            coEvery { service.get(any()) } returns Erc20Token(Address.FOUR(), "Test", "Test", 18)

            return service
        }
    }
}
