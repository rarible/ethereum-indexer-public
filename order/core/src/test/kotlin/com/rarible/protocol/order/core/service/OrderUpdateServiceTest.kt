package com.rarible.protocol.order.core.service

import com.rarible.protocol.order.core.data.createOrderVersion
import com.rarible.protocol.order.core.data.randomOrder
import com.rarible.protocol.order.core.event.OrderListener
import com.rarible.protocol.order.core.event.OrderVersionListener
import com.rarible.protocol.order.core.misc.orderOffchainEventMarks
import com.rarible.protocol.order.core.provider.ProtocolCommissionProvider
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.repository.order.OrderVersionRepository
import com.rarible.protocol.order.core.service.balance.AssetMakeBalanceProvider
import io.daonomic.rpc.domain.WordFactory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono

internal class OrderUpdateServiceTest {
    private val orderRepository = mockk<OrderRepository>()
    private val assetMakeBalanceProvider = mockk<AssetMakeBalanceProvider>()
    private val orderVersionRepository = mockk<OrderVersionRepository>()
    private val orderReduceService = mockk<OrderReduceService>()
    private val protocolCommissionProvider = mockk<ProtocolCommissionProvider>()
    private val priceUpdateService = mockk<PriceUpdateService>()
    private val orderVersionListener = mockk<OrderVersionListener>()
    private val orderListener = mockk<OrderListener>()

    private val orderUpdateService = OrderUpdateService(
        orderRepository,
        assetMakeBalanceProvider,
        orderVersionRepository,
        orderReduceService,
        protocolCommissionProvider,
        priceUpdateService,
        orderVersionListener,
        orderListener,
        emptyList()
    )

    @Test
    fun `should send event if lastEventId was changed`() = runBlocking<Unit> {
        val hash = WordFactory.create()
        val order = randomOrder().copy(lastEventId = "1")
        val updatedOrder = randomOrder().copy(lastEventId = "2")

        coEvery { orderRepository.findById(eq(hash)) } returns order
        coEvery { orderReduceService.updateOrder(eq(hash)) } returns updatedOrder
        coEvery { orderListener.onOrder(eq(updatedOrder), any()) } returns Unit

        orderUpdateService.update(hash, orderOffchainEventMarks())

        coVerify { orderListener.onOrder(eq(updatedOrder), any()) }
    }

    @Test
    fun `update approval`() = runBlocking<Unit> {
        val order = randomOrder()
        val updatedOrder = randomOrder()
        val orderVersion = createOrderVersion()
        val updatedVersion = createOrderVersion()
        coEvery { orderVersionRepository.findLatestByHash(order.hash) } returns orderVersion
        every { orderVersionRepository.save(orderVersion.copy(approved = false)) } returns Mono.just(updatedVersion)
        coEvery { orderReduceService.updateOrder(order.hash) } returns updatedOrder
        coEvery { orderListener.onOrder(eq(updatedOrder), any()) } returns Unit

        orderUpdateService.updateApproval(order = order, approved = false, eventTimeMarks = orderOffchainEventMarks())

        verify { orderVersionRepository.save(orderVersion.copy(approved = false)) }
        coVerify { orderReduceService.updateOrder(order.hash) }
    }
}
