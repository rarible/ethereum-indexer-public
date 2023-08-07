package com.rarible.protocol.order.listener.service.x2y2

import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.core.service.x2y2.X2Y2Service
import com.rarible.protocol.order.listener.configuration.X2Y2OrderLoadProperties
import com.rarible.protocol.order.listener.data.createOrder
import com.rarible.protocol.order.listener.data.createOrderVersion
import com.rarible.protocol.order.listener.data.randomX2Y2Order
import com.rarible.protocol.order.core.metric.ForeignOrderMetrics
import com.rarible.x2y2.client.model.ApiListResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

internal class X2Y2OrderLoaderTest {

    private val x2y2OrderService = mockk<X2Y2Service>()
    private val x2Y2OrderConverter = mockk<X2Y2OrderConverter>()
    private val orderRepository = mockk<OrderRepository>()
    private val orderUpdateService = mockk<OrderUpdateService>()
    private val properties = X2Y2OrderLoadProperties(saveEnabled = true)
    private val metrics: ForeignOrderMetrics = mockk() {
        coEvery { onDownloadedOrderHandled(Platform.X2Y2) } returns Unit
        coEvery { onOrderReceived(Platform.X2Y2, any()) } returns Unit
        every { onLatestOrderReceived(Platform.X2Y2, any()) } returns Unit
    }

    private val handler = X2Y2OrderLoader(
        x2y2OrderService,
        x2Y2OrderConverter,
        orderRepository,
        orderUpdateService,
        properties,
        metrics
    )

    @Test
    fun `should get filter save new x2y2 orders`() = runBlocking<Unit> {
        val validClientOrder1 = randomX2Y2Order()
        val validOrderVersion1 = createOrderVersion()
        val validOrder1 = createOrder()

        val validClientOrder2 = randomX2Y2Order()
        val validOrderVersion2 = createOrderVersion()
        val validOrder2 = createOrder()

        val validClientOrder3 = randomX2Y2Order()
        val validOrderVersion3 = createOrderVersion()

        val x2y2Orders = ApiListResponse(
            next = "next",
            success = true,
            data = listOf(validClientOrder1, validClientOrder2, validClientOrder3)
        )
        coEvery { x2y2OrderService.getNextSellOrders(any()) } returns x2y2Orders

        coEvery { x2Y2OrderConverter.convert(validClientOrder1) } returns validOrderVersion1
        coEvery { x2Y2OrderConverter.convert(validClientOrder2) } returns validOrderVersion2
        coEvery { x2Y2OrderConverter.convert(validClientOrder3) } returns validOrderVersion3

        coEvery { orderRepository.findById(validOrderVersion1.hash) } returns null
        coEvery { orderRepository.findById(validOrderVersion2.hash) } returns null
        coEvery { orderRepository.findById(validOrderVersion3.hash) } returns createOrder()

        coEvery { orderUpdateService.save(eq(validOrderVersion1), any()) } returns validOrder1
        coEvery { orderUpdateService.updateMakeStock(eq(validOrder1), any(), any()) } returns (validOrder1 to true)
        coEvery { orderUpdateService.save(eq(validOrderVersion2), any()) } returns validOrder2
        coEvery { orderUpdateService.updateMakeStock(eq(validOrder2), any(), any()) } returns (validOrder2 to true)

        handler.load(null)

        coVerify(exactly = 1) { x2y2OrderService.getNextSellOrders(any()) }
        coVerify(exactly = 3) { x2Y2OrderConverter.convert(any()) }

        coVerify(exactly = 3) { orderRepository.findById(any()) }
        coVerify(exactly = 1) { orderRepository.findById(validOrderVersion1.hash) }
        coVerify(exactly = 1) { orderRepository.findById(validOrderVersion2.hash) }
        coVerify(exactly = 1) { orderRepository.findById(validOrderVersion3.hash) }

        coVerify(exactly = 2) { orderUpdateService.save(any(), any()) }
        coVerify(exactly = 1) { orderUpdateService.save(eq(validOrderVersion1), any()) }
        coVerify(exactly = 1) { orderUpdateService.save(eq(validOrderVersion2), any()) }
    }
}
