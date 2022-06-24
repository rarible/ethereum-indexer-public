package com.rarible.protocol.order.listener.service.opensea

import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.opensea.client.model.v2.SeaportOrder
import com.rarible.opensea.client.model.v2.SeaportOrders
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.listener.configuration.SeaportLoadProperties
import com.rarible.protocol.order.listener.data.createOrder
import com.rarible.protocol.order.listener.data.createOrderVersion
import com.rarible.protocol.order.listener.data.randomSeaportOrder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

internal class SeaportOrderLoadHandlerTest {
    private val openSeaOrderService = mockk<OpenSeaOrderService>()
    private val openSeaOrderConverter = mockk<OpenSeaOrderConverter>()
    private val openSeaOrderValidator = mockk<OpenSeaOrderValidator>()
    private val orderRepository = mockk<OrderRepository>()
    private val orderUpdateService = mockk<OrderUpdateService>()
    private val seaportSaveCounter = mockk<RegisteredCounter>()

    private val handler =  SeaportOrderLoadHandler(
        openSeaOrderService = openSeaOrderService,
        openSeaOrderConverter = openSeaOrderConverter,
        openSeaOrderValidator = openSeaOrderValidator,
        orderRepository = orderRepository,
        orderUpdateService = orderUpdateService,
        properties = SeaportLoadProperties(saveEnabled = true),
        seaportSaveCounter = seaportSaveCounter
    )

    @Test
    fun `should get filter save new seaport orders`() = runBlocking<Unit> {
        val validClientOrder1 = randomSeaportOrder()
        val validOrderVersion1 = createOrderVersion()

        val validClientOrder2 = randomSeaportOrder()
        val validOrderVersion2 = createOrderVersion()

        val invalidClientOrder1 = randomSeaportOrder()
        val invalidClientOrder2 = randomSeaportOrder()
        val invalidOrderVersion2 = createOrderVersion()
        val seaportOrders = SeaportOrders(
            next = null,
            previous = null,
            orders = listOf(validClientOrder1, validClientOrder2, invalidClientOrder1, invalidClientOrder2)
        )
        coEvery { openSeaOrderService.getNextSellOrders() } returns seaportOrders

        coEvery { openSeaOrderConverter.convert(validClientOrder1) } returns validOrderVersion1
        coEvery { openSeaOrderConverter.convert(validClientOrder2) } returns validOrderVersion2
        coEvery { openSeaOrderConverter.convert(invalidClientOrder1) } returns null
        coEvery { openSeaOrderConverter.convert(invalidClientOrder2) } returns invalidOrderVersion2

        every { openSeaOrderValidator.validate(validOrderVersion1) } returns true
        every { openSeaOrderValidator.validate(validOrderVersion2) } returns true
        every { openSeaOrderValidator.validate(invalidOrderVersion2) } returns false

        coEvery { orderRepository.findById(validOrderVersion1.hash) } returns null
        coEvery { orderRepository.findById(validOrderVersion2.hash) } returns createOrder()

        coEvery { orderUpdateService.save(validOrderVersion1) } returns createOrder()
        every { seaportSaveCounter.increment() } returns Unit

        handler.handle()

        coVerify(exactly = 1) { openSeaOrderService.getNextSellOrders() }
        coVerify(exactly = 4) { openSeaOrderConverter.convert(any<SeaportOrder>()) }
        coVerify(exactly = 3) { openSeaOrderValidator.validate(any()) }

        coVerify(exactly = 2) { orderRepository.findById(any()) }
        coVerify(exactly = 1) { orderRepository.findById(validOrderVersion1.hash) }
        coVerify(exactly = 1) { orderRepository.findById(validOrderVersion2.hash) }

        coVerify(exactly = 1) { orderUpdateService.save(any()) }
        coVerify(exactly = 1) { orderUpdateService.save(validOrderVersion1) }
        verify(exactly = 1) { seaportSaveCounter.increment() }
    }
}