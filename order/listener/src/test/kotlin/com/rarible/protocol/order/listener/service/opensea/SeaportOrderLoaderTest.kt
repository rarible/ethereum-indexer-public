package com.rarible.protocol.order.listener.service.opensea

import com.rarible.opensea.client.model.v2.SeaportOrder
import com.rarible.opensea.client.model.v2.SeaportOrders
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.listener.configuration.SeaportLoadProperties
import com.rarible.protocol.order.listener.data.createOrder
import com.rarible.protocol.order.listener.data.createOrderVersion
import com.rarible.protocol.order.listener.data.randomSeaportOrder
import com.rarible.protocol.order.core.metric.ForeignOrderMetrics
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class SeaportOrderLoaderTest {

    private val openSeaOrderService = mockk<OpenSeaOrderService>()
    private val openSeaOrderConverter = mockk<OpenSeaOrderConverter>()
    private val openSeaOrderValidator = mockk<OpenSeaOrderValidator>()
    private val orderRepository = mockk<OrderRepository>()
    private val orderUpdateService = mockk<OrderUpdateService>()
    private val metrics = mockk<ForeignOrderMetrics>() {
        every { onDownloadedOrderHandled(Platform.OPEN_SEA) } returns Unit
        every { onDownloadedOrderSkipped(Platform.OPEN_SEA, any()) } returns Unit
        every { onDownloadedOrderError(Platform.OPEN_SEA, any()) } returns Unit
    }
    private val properties = SeaportLoadProperties(saveEnabled = true)

    private val handler = SeaportOrderLoader(
        openSeaOrderService = openSeaOrderService,
        openSeaOrderConverter = openSeaOrderConverter,
        openSeaOrderValidator = openSeaOrderValidator,
        orderRepository = orderRepository,
        orderUpdateService = orderUpdateService,
        properties = properties,
        metrics = metrics
    )

    @Test
    fun `should get filter save new seaport orders`() = runBlocking<Unit> {
        val validClientOrder1 = randomSeaportOrder()
        val validOrderVersion1 = createOrderVersion()
        val order1 = createOrder()

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

        coEvery { orderUpdateService.save(eq(validOrderVersion1), any()) } returns order1
        coEvery { orderUpdateService.updateMakeStock(order1, any(), any()) } returns (order1 to true)

        handler.load(null, false)

        coVerify(exactly = 1) { openSeaOrderService.getNextSellOrders(any()) }
        coVerify(exactly = 4) { openSeaOrderConverter.convert(any<SeaportOrder>()) }
        coVerify(exactly = 3) { openSeaOrderValidator.validate(any()) }

        coVerify(exactly = 2) { orderRepository.findById(any()) }
        coVerify(exactly = 1) { orderRepository.findById(validOrderVersion1.hash) }
        coVerify(exactly = 1) { orderRepository.findById(validOrderVersion2.hash) }

        coVerify(exactly = 1) { orderUpdateService.save(any(), any()) }
        coVerify(exactly = 1) { orderUpdateService.save(eq(validOrderVersion1), any()) }
    }

    @Test
    fun `should get and save all new seaport orders while previas is not null`() = runBlocking<Unit> {
        val clientOrder1 = randomSeaportOrder()
        val orderVersion1 = createOrderVersion()
        val order1 = createOrder()

        val clientOrder2 = randomSeaportOrder()
        val orderVersion2 = createOrderVersion()
        val order2 = createOrder()

        val clientOrder3 = randomSeaportOrder()
        val orderVersion3 = createOrderVersion()
        val order3 = createOrder()

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

        coEvery { orderUpdateService.save(eq(orderVersion1), any()) } returns order1
        coEvery { orderUpdateService.save(eq(orderVersion2), any()) } returns order2
        coEvery { orderUpdateService.save(eq(orderVersion3), any()) } returns order3

        coEvery { orderUpdateService.updateMakeStock(order1, any(), any()) } returns (order1 to true)
        coEvery { orderUpdateService.updateMakeStock(order2, any(), any()) } returns (order2 to true)
        coEvery { orderUpdateService.updateMakeStock(order3, any(), any()) } returns (order3 to true)

        val result = handler.load(previous, false)
        assertThat(result).isEqualTo(seaportOrders3)

        coVerifyOrder {
            openSeaOrderService.getNextSellOrders(previous)
            openSeaOrderService.getNextSellOrders(seaportOrders1.previous)
            openSeaOrderService.getNextSellOrders(seaportOrders2.previous)
        }
        coVerify(exactly = 1) { orderUpdateService.save(eq(orderVersion1), any()) }
        coVerify(exactly = 1) { orderUpdateService.save(eq(orderVersion2), any()) }
        coVerify(exactly = 1) { orderUpdateService.save(eq(orderVersion3), any()) }
    }

    @Test
    fun `should throw exception if client fail`() = runBlocking<Unit> {
        val clientOrder1 = randomSeaportOrder()
        val orderVersion1 = createOrderVersion()

        val previous = "previous0"

        val seaportOrders1 = SeaportOrders(
            next = "next1",
            previous = "previous1",
            orders = listOf(clientOrder1)
        )
        coEvery { openSeaOrderService.getNextSellOrders(previous) } returns seaportOrders1
        coEvery { openSeaOrderService.getNextSellOrders(seaportOrders1.previous) } throws RuntimeException("Fail")

        coEvery { openSeaOrderConverter.convert(clientOrder1) } returns orderVersion1
        every { openSeaOrderValidator.validate(orderVersion1) } returns true
        coEvery { orderRepository.findById(orderVersion1.hash) } returns null
        coEvery { orderUpdateService.save(orderVersion1, any()) } returns createOrder()
        coEvery { orderUpdateService.updateMakeStock(eq(orderVersion1.hash), any(), any()) } returns mockk()

        assertThrows<RuntimeException> {
            runBlocking {
                handler.load(previous, false)
            }
        }
    }

    @Test
    fun `should get only maxLoadResults`() = runBlocking<Unit> {
        for (i in 1..(properties.maxLoadResults * 2)) {
            val clientOrder = randomSeaportOrder()
            val orderVersion = createOrderVersion()
            val order = createOrder()

            val seaportOrders = SeaportOrders(
                next = "next$i",
                previous = "previous$i",
                orders = listOf(clientOrder)
            )
            coEvery { openSeaOrderService.getNextSellOrders("previous${i - 1}") } returns seaportOrders
            coEvery { openSeaOrderConverter.convert(clientOrder) } returns orderVersion
            every { openSeaOrderValidator.validate(orderVersion) } returns true
            coEvery { orderRepository.findById(orderVersion.hash) } returns null
            coEvery { orderUpdateService.save(eq(orderVersion), any()) } returns order
            coEvery { orderUpdateService.updateMakeStock(eq(order), any(), any()) } returns (order to true)
        }

        val result = handler.load("previous0", false)
        assertThat(result.previous).isEqualTo("previous${properties.maxLoadResults}")
        coVerify(exactly = properties.maxLoadResults) { openSeaOrderService.getNextSellOrders(any()) }
        coVerify(exactly = properties.maxLoadResults) { orderUpdateService.save(any(), any()) }
    }
}
