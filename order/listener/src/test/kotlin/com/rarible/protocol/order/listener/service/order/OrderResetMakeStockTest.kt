package com.rarible.protocol.order.listener.service.order

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.Erc20BalanceDto
import com.rarible.protocol.dto.Erc20BalanceUpdateEventDto
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Erc1155AssetType
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.listener.configuration.OrderListenerProperties
import com.rarible.protocol.order.listener.data.createOrderVersion
import com.rarible.protocol.order.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.order.listener.integration.IntegrationTest
import com.rarible.protocol.order.listener.job.OrderRecalculateMakeStockJob
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import scalether.domain.AddressFactory

@IntegrationTest
@FlowPreview
@Disabled // TODO: enable the test after release. It is flaky.
internal class OrderResetMakeStockTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var orderBalanceService: OrderBalanceService

    @Autowired
    private lateinit var reactiveMongoTemplate: ReactiveMongoTemplate

    @Test
    fun `should reset makeStock`() = runBlocking<Unit> {
        val props = OrderListenerProperties(resetMakeStockEnabled = true)
        val updaterJob = OrderRecalculateMakeStockJob(props, reactiveMongoTemplate, orderUpdateService)
        val targetMaker = AddressFactory.create()
        val targetToken = AddressFactory.create()
        val stock = EthUInt256.of(5)
        val make = Asset(Erc20AssetType(targetToken), EthUInt256.TEN)
        val take = Asset(Erc1155AssetType(AddressFactory.create(), EthUInt256.TEN), EthUInt256.TEN)

        val order1 = createOrderVersion().copy(
            maker = targetMaker,
            make = make,
            take = take,
            start = 0L,
            end = Long.MAX_VALUE
        )

        listOf(order1).forEach { orderUpdateService.save(it) }

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

        // override the end date
        val order = orderRepository.findById(order1.hash)!!
        orderRepository.save(order.copy(end = order.start))

        updaterJob.update()
        assertThat(orderRepository.findById(order1.hash)?.makeStock).isEqualTo(EthUInt256.ZERO)
    }

    @Test
    fun `should recalculate makeStock`() = runBlocking<Unit> {
        val props = OrderListenerProperties(resetMakeStockEnabled = true)
        val updaterJob = OrderRecalculateMakeStockJob(props, reactiveMongoTemplate, orderUpdateService)
        val targetMaker = AddressFactory.create()
        val targetToken = AddressFactory.create()
        val stock = EthUInt256.of(5)
        val make = Asset(Erc20AssetType(targetToken), EthUInt256.TEN)
        val take = Asset(Erc1155AssetType(AddressFactory.create(), EthUInt256.TEN), EthUInt256.TEN)

        val order1 = createOrderVersion().copy(
            maker = targetMaker,
            make = make,
            take = take,
            start = 0L,
            end = 0L
        )

        listOf(order1).forEach { orderUpdateService.save(it) }

        val updatedBalance = mockk<Erc20BalanceDto> {
            every { owner } returns targetMaker
            every { contract } returns targetToken
            every { balance } returns stock.value
        }
        val event = mockk<Erc20BalanceUpdateEventDto> {
            every { balance } returns updatedBalance
        }

        orderBalanceService.handle(event)

        assertThat(orderRepository.findById(order1.hash)?.makeStock).isEqualTo(EthUInt256.ZERO)

        // override the end date
        val order = orderRepository.findById(order1.hash)!!
        orderRepository.save(order.copy(end = Long.MAX_VALUE))

        updaterJob.update()
        assertThat(orderRepository.findById(order1.hash)?.makeStock).isEqualTo(EthUInt256.ONE)
    }
}
