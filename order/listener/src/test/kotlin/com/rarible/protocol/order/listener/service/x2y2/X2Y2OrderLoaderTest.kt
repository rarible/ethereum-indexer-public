package com.rarible.protocol.order.listener.service.x2y2

import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.listener.configuration.X2Y2LoadProperties
import com.rarible.protocol.order.listener.data.createOrder
import com.rarible.protocol.order.listener.data.createOrderVersion
import com.rarible.protocol.order.listener.data.randomX2Y2Order
import com.rarible.x2y2.client.model.ApiListResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

internal class X2Y2OrderLoaderTest {
    private val x2y2OrderService = mockk<X2Y2OrderService>()
    private val x2Y2OrderConverter = mockk<X2Y2OrderConverter>()
    private val orderRepository = mockk<OrderRepository>()
    private val orderUpdateService = mockk<OrderUpdateService>()
    private val properties = X2Y2LoadProperties(saveEnabled = true)
    private val x2y2SaveCounter = mockk<RegisteredCounter>()

    private val handler =  X2Y2OrderLoader(
        x2y2OrderService,
        x2Y2OrderConverter,
        orderRepository,
        orderUpdateService,
        properties,
        x2y2SaveCounter
    )

    @Test
    fun `should get filter save new seaport orders`() = runBlocking<Unit> {
        val validClientOrder1 = randomX2Y2Order()
        val validOrderVersion1 = createOrderVersion()

        val validClientOrder2 = randomX2Y2Order()
        val validOrderVersion2 = createOrderVersion()

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

        coEvery { orderUpdateService.save(validOrderVersion1) } returns createOrder()
        coEvery { orderUpdateService.save(validOrderVersion2) } returns createOrder()
        every { x2y2SaveCounter.increment() } returns Unit

        handler.load(null)

        coVerify(exactly = 1) { x2y2OrderService.getNextSellOrders(any()) }
        coVerify(exactly = 3) { x2Y2OrderConverter.convert(any()) }

        coVerify(exactly = 3) { orderRepository.findById(any()) }
        coVerify(exactly = 1) { orderRepository.findById(validOrderVersion1.hash) }
        coVerify(exactly = 1) { orderRepository.findById(validOrderVersion2.hash) }
        coVerify(exactly = 1) { orderRepository.findById(validOrderVersion3.hash) }

        coVerify(exactly = 2) { orderUpdateService.save(any()) }
        coVerify(exactly = 1) { orderUpdateService.save(validOrderVersion1) }
        coVerify(exactly = 1) { orderUpdateService.save(validOrderVersion2) }
        verify(exactly = 2) { x2y2SaveCounter.increment() }
    }
}