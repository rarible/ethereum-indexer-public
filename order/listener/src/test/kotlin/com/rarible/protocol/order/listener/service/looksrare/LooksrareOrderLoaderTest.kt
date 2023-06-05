package com.rarible.protocol.order.listener.service.looksrare

import com.rarible.core.telemetry.metrics.RegisteredGauge
import com.rarible.looksrare.client.model.v2.Status
import com.rarible.protocol.order.core.model.LooksrareV2Cursor
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.listener.configuration.LooksrareLoadProperties
import com.rarible.protocol.order.listener.data.createOrder
import com.rarible.protocol.order.listener.data.createOrderVersion
import com.rarible.protocol.order.listener.data.randomLooksrareOrder
import com.rarible.protocol.order.listener.misc.ForeignOrderMetrics
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

internal class LooksrareOrderLoaderTest {

    private val looksrareOrderService = mockk<LooksrareOrderService>()
    private val looksrareOrderConverter = mockk<LooksrareOrderConverter>()
    private val orderRepository = mockk<OrderRepository>()
    private val orderUpdateService = mockk<OrderUpdateService>()
    private val properties = LooksrareLoadProperties()
    private val metrics = mockk<ForeignOrderMetrics> {
        every { onDownloadedOrderHandled(Platform.LOOKSRARE) } returns Unit
        every { onOrderReceived(Platform.LOOKSRARE, any()) } returns Unit
    }
    private val looksrareOrderDelayGauge = mockk<RegisteredGauge<Long>> { every { set(any()) } returns Unit }

    private val loader = LooksrareOrderLoader(
        looksrareOrderService,
        looksrareOrderConverter,
        orderRepository,
        orderUpdateService,
        properties,
        metrics
    )

    @Test
    fun `should convert and save a new looksrare order`() = runBlocking<Unit> {
        properties.saveEnabled = true
        val looksrareOrder1 = randomLooksrareOrder().copy(createdAt = Instant.now().minusSeconds(10), status = Status.VALID)
        val looksrareOrder2 = randomLooksrareOrder().copy(createdAt = Instant.now().minusSeconds(5), status = Status.VALID)
        val orderVersion1 = createOrderVersion().copy(hash = looksrareOrder1.hash)
        val orderVersion2 = createOrderVersion().copy(hash = looksrareOrder2.hash)
        val order1 = createOrder().copy(id = Order.Id(looksrareOrder1.hash), hash = looksrareOrder1.hash)
        val order2 = createOrder().copy(id = Order.Id(looksrareOrder2.hash), hash = looksrareOrder2.hash)
        val listedAfter = LooksrareV2Cursor(Instant.now())

        coEvery { looksrareOrderService.getNextSellOrders(listedAfter) } returns listOf(
            looksrareOrder1,
            looksrareOrder2
        )
        coEvery { orderRepository.findById(looksrareOrder1.hash) } returns null
        coEvery { orderRepository.findById(looksrareOrder2.hash) } returns null
        coEvery { looksrareOrderConverter.convert(looksrareOrder1) } returns orderVersion1
        coEvery { looksrareOrderConverter.convert(looksrareOrder2) } returns orderVersion2
        coEvery { orderUpdateService.save(eq(orderVersion1), any()) } returns order1
        coEvery { orderUpdateService.updateMakeStock(eq(order1), any(), any()) } returns (order1 to true)
        coEvery { orderUpdateService.save(eq(orderVersion2), any()) } returns order2
        coEvery { orderUpdateService.updateMakeStock(order2, any(), any()) } returns (order2 to true)

        val result = loader.load(listedAfter)
        assertThat(result.cursor?.createdAfter).isEqualTo(looksrareOrder2.createdAt)

        coVerify(exactly = 1) { orderUpdateService.save(eq(orderVersion1), any()) }
        coVerify(exactly = 1) { orderUpdateService.save(eq(orderVersion2), any()) }
        coVerify(exactly = 1) { looksrareOrderConverter.convert(looksrareOrder1) }
        coVerify(exactly = 1) { looksrareOrderConverter.convert(looksrareOrder2) }
    }

    @Test
    fun `should not save a new looksrare order`() = runBlocking<Unit> {
        properties.saveEnabled = false
        val looksrareOrder1 = randomLooksrareOrder()
        val orderVersion1 = createOrderVersion().copy(hash = looksrareOrder1.hash)
        val listedAfter = LooksrareV2Cursor(Instant.now())

        coEvery { looksrareOrderService.getNextSellOrders(listedAfter) } returns listOf(looksrareOrder1)
        coEvery { looksrareOrderConverter.convert(looksrareOrder1) } returns orderVersion1
        coVerify(exactly = 0) { orderUpdateService.save(orderVersion1) }
    }
}
