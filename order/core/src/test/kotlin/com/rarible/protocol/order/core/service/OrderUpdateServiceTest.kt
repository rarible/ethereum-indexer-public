package com.rarible.protocol.order.core.service

import com.rarible.protocol.order.core.data.createOrder
import com.rarible.protocol.order.core.event.OrderListener
import com.rarible.protocol.order.core.event.OrderVersionListener
import com.rarible.protocol.order.core.provider.ProtocolCommissionProvider
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.repository.order.OrderVersionRepository
import com.rarible.protocol.order.core.service.balance.AssetMakeBalanceProvider
import io.daonomic.rpc.domain.WordFactory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

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
        orderListener
    )

    @Test
    fun `should send event if lastEventId was changed`() = runBlocking<Unit> {
        val hash =WordFactory.create()
        val order = createOrder().copy(lastEventId = "1")
        val updatedOrder = createOrder().copy(lastEventId = "2")

        coEvery { orderRepository.findById(eq(hash)) } returns order
        coEvery { orderReduceService.updateOrder(eq(hash)) } returns updatedOrder
        coEvery { orderListener.onOrder(eq(updatedOrder)) } returns Unit

        orderUpdateService.update(hash)

        coVerify { orderListener.onOrder(eq(updatedOrder)) }
    }

    @Test
    fun `should not send event if lastEventId was not changed`() = runBlocking<Unit> {
        val hash =WordFactory.create()
        val order = createOrder().copy(lastEventId = "2")
        val updatedOrder = createOrder().copy(lastEventId = "2")

        coEvery { orderRepository.findById(eq(hash)) } returns order
        coEvery { orderReduceService.updateOrder(eq(hash)) } returns updatedOrder

        orderUpdateService.update(hash)

        coVerify(exactly = 0) { orderListener.onOrder(any()) }
    }
}
