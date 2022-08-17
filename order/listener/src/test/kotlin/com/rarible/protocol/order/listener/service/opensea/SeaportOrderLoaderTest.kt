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
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class eaportOrderLoaderTest {
    private val openSeaOrderService = mockk<OpenSeaOrderService>()
    private val openSeaOrderConverter = mockk<OpenSeaOrderConverter>()
    private val openSeaOrderValidator = mockk<OpenSeaOrderValidator>()
    private val orderRepository = mockk<OrderRepository>()
    private val orderUpdateService = mockk<OrderUpdateService>()
    private val seaportSaveCounter = mockk<RegisteredCounter>()

    private val handler =  SeaportOrderLoader(
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
            next = "",
            previous = null,
            orders = listOf(validClientOrder1, validClientOrder2, invalidClientOrder1, invalidClientOrder2)
        )
        coEvery { openSeaOrderService.getNextSellOrders(any()) } returns seaportOrders

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
        coEvery { orderUpdateService.updateMakeStock(validOrderVersion1.hash) } returns mockk()
        every { seaportSaveCounter.increment() } returns Unit

        handler.load(null)

        coVerify(exactly = 1) { openSeaOrderService.getNextSellOrders(any()) }
        coVerify(exactly = 4) { openSeaOrderConverter.convert(any<SeaportOrder>()) }
        coVerify(exactly = 3) { openSeaOrderValidator.validate(any()) }

        coVerify(exactly = 2) { orderRepository.findById(any()) }
        coVerify(exactly = 1) { orderRepository.findById(validOrderVersion1.hash) }
        coVerify(exactly = 1) { orderRepository.findById(validOrderVersion2.hash) }

        coVerify(exactly = 1) { orderUpdateService.save(any()) }
        coVerify(exactly = 1) { orderUpdateService.save(validOrderVersion1) }
        verify(exactly = 1) { seaportSaveCounter.increment() }
    }

    @Test
    fun `should get and save all new seaport orders while previas is not null`() = runBlocking<Unit> {
        val clientOrder1 = randomSeaportOrder()
        val orderVersion1 = createOrderVersion()

        val clientOrder2 = randomSeaportOrder()
        val orderVersion2 = createOrderVersion()

        val clientOrder3 = randomSeaportOrder()
        val orderVersion3 = createOrderVersion()

        val previous = "previous0"

        val seaportOrders1 = SeaportOrders(
            next = "next1",
            previous = "previous1",
            orders = listOf(clientOrder1)
        )
        val seaportOrders2 = SeaportOrders(
            next = "next2",
            previous = "previous2",
            orders = listOf(clientOrder2)
        )
        val seaportOrders3 = SeaportOrders(
            next = "next3",
            previous = null,
            orders = listOf(clientOrder3)
        )
        coEvery { openSeaOrderService.getNextSellOrders(previous) } returns seaportOrders1
        coEvery { openSeaOrderService.getNextSellOrders(seaportOrders1.previous) } returns seaportOrders2
        coEvery { openSeaOrderService.getNextSellOrders(seaportOrders2.previous) } returns seaportOrders3

        coEvery { openSeaOrderConverter.convert(clientOrder1) } returns orderVersion1
        coEvery { openSeaOrderConverter.convert(clientOrder2) } returns orderVersion2
        coEvery { openSeaOrderConverter.convert(clientOrder3) } returns orderVersion3

        every { openSeaOrderValidator.validate(orderVersion1) } returns true
        every { openSeaOrderValidator.validate(orderVersion2) } returns true
        every { openSeaOrderValidator.validate(orderVersion3) } returns true

        coEvery { orderRepository.findById(orderVersion1.hash) } returns null
        coEvery { orderRepository.findById(orderVersion2.hash) } returns null
        coEvery { orderRepository.findById(orderVersion3.hash) } returns null

        coEvery { orderUpdateService.save(orderVersion1) } returns createOrder()
        coEvery { orderUpdateService.save(orderVersion2) } returns createOrder()
        coEvery { orderUpdateService.save(orderVersion3) } returns createOrder()

        coEvery { orderUpdateService.updateMakeStock(orderVersion1.hash) } returns mockk()
        coEvery { orderUpdateService.updateMakeStock(orderVersion2.hash) } returns mockk()
        coEvery { orderUpdateService.updateMakeStock(orderVersion3.hash) } returns mockk()
        every { seaportSaveCounter.increment() } returns Unit

        val result = handler.load(previous)
        assertThat(result).isEqualTo(seaportOrders3)

        coVerifyOrder {
            openSeaOrderService.getNextSellOrders(previous)
            openSeaOrderService.getNextSellOrders(seaportOrders1.previous)
            openSeaOrderService.getNextSellOrders(seaportOrders2.previous)
        }
        coVerify(exactly = 1) { orderUpdateService.save(orderVersion1) }
        coVerify(exactly = 1) { orderUpdateService.save(orderVersion2) }
        coVerify(exactly = 1) { orderUpdateService.save(orderVersion3) }
    }
}