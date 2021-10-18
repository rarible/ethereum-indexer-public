package com.rarible.protocol.order.listener.service.order

import com.rarible.core.common.nowMillis
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
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import scalether.domain.AddressFactory
import java.time.Duration

@IntegrationTest
@FlowPreview
internal class OrderResetMakeStockTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var orderBalanceService: OrderBalanceService

    @Autowired
    private lateinit var reactiveMongoTemplate: ReactiveMongoTemplate

    private val updaterJob
        get() = OrderRecalculateMakeStockJob(
            OrderListenerProperties(resetMakeStockEnabled = true),
            reactiveMongoTemplate,
            orderUpdateService
        )

    @Test
    fun `should reset makeStock to 0 when order is expired`() = runBlocking<Unit> {
        val targetMaker = AddressFactory.create()
        val targetToken = AddressFactory.create()
        val make = Asset(Erc20AssetType(targetToken), EthUInt256.TEN)
        val take = Asset(Erc1155AssetType(AddressFactory.create(), EthUInt256.TEN), EthUInt256.TEN)
        val orderVersion = createOrderVersion().copy(
            maker = targetMaker,
            make = make,
            take = take,
            start = nowMillis().epochSecond,
            end = nowMillis().plus(Duration.ofHours(1)).epochSecond
        )

        clearMocks(assetMakeBalanceProvider)
        coEvery { assetMakeBalanceProvider.getMakeBalance(any()) } returns EthUInt256.ZERO

        val newStock = EthUInt256.of(5)
        val order = orderUpdateService.save(orderVersion)
        val updatedBalance = mockk<Erc20BalanceDto> {
            every { owner } returns targetMaker
            every { contract } returns targetToken
            every { balance } returns newStock.value
        }
        val event = mockk<Erc20BalanceUpdateEventDto> {
            every { balance } returns updatedBalance
        }
        orderBalanceService.handle(event)
        assertThat(orderRepository.findById(orderVersion.hash)?.makeStock).isEqualTo(newStock)

        updaterJob.update(nowMillis().plus(Duration.ofHours(2))) // As if the order is expired.
        assertThat(orderRepository.findById(order.hash)?.makeStock).isEqualTo(EthUInt256.ZERO)
    }

    @Test
    fun `should recalculate makeStock if it is 0 and the order is still actual`() = runBlocking<Unit> {
        val newStock = EthUInt256.of(5)
        val targetMaker = AddressFactory.create()
        val targetToken = AddressFactory.create()
        val make = Asset(Erc20AssetType(targetToken), EthUInt256.TEN)
        val take = Asset(Erc1155AssetType(AddressFactory.create(), EthUInt256.TEN), EthUInt256.TEN)
        val orderVersion = createOrderVersion().copy(
            maker = targetMaker,
            make = make,
            take = take,
            start = 0,
            end = Long.MAX_VALUE
        )

        clearMocks(assetMakeBalanceProvider)
        coEvery { assetMakeBalanceProvider.getMakeBalance(any()) } returns EthUInt256.ZERO
        orderUpdateService.save(orderVersion)
        assertThat(orderRepository.findById(orderVersion.hash)?.makeStock).isEqualTo(EthUInt256.ZERO)

        clearMocks(assetMakeBalanceProvider)
        coEvery { assetMakeBalanceProvider.getMakeBalance(any()) } returns newStock
        updaterJob.update()
        assertThat(orderRepository.findById(orderVersion.hash)?.makeStock).isEqualTo(newStock)
    }
}
