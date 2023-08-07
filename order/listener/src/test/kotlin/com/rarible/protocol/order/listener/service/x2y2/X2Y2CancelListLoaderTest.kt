package com.rarible.protocol.order.listener.service.x2y2

import com.rarible.core.test.data.randomString
import com.rarible.protocol.order.core.data.createSellOrder
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.repository.order.OrderStateRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.core.service.x2y2.X2Y2Service
import com.rarible.protocol.order.listener.data.randomX2Y2Event
import com.rarible.protocol.order.core.metric.ForeignOrderMetrics
import com.rarible.x2y2.client.model.ApiListResponse
import com.rarible.x2y2.client.model.EventType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class X2Y2CancelListLoaderTest {

    private val x2y2Service = mockk<X2Y2Service>()
    private val orderStateRepository = mockk<OrderStateRepository>()
    private val orderUpdateService = mockk<OrderUpdateService>()
    private val metrics = mockk<ForeignOrderMetrics> {
        every { onOrderEventHandled(Platform.X2Y2, "cancel_offchain") } returns Unit
        every { onOrderReceived(Platform.X2Y2, any()) } returns Unit
        every { onOrderReceived(Platform.X2Y2, any(), any()) } returns Unit
    }

    private val handler = X2Y2CancelListEventLoader(
        x2y2Service,
        orderStateRepository,
        orderUpdateService,
        metrics
    )

    @Test
    fun `should save cancels save for x2y2 order`() = runBlocking<Unit> {
        val offChainEvent1 = randomX2Y2Event().copy(tx = null)
        val offChainEvent2 = randomX2Y2Event().copy(tx = null)
        val offChainEvent3 = randomX2Y2Event().copy(tx = null)
        val onChainEvent1 = randomX2Y2Event().copy(tx = randomString())
        val onChainEvent2 = randomX2Y2Event().copy(tx = randomString())

        val x2y2Event = ApiListResponse(
            next = "next",
            success = true,
            data = listOf(onChainEvent1, offChainEvent3, offChainEvent2, offChainEvent1, onChainEvent2)
        )
        coEvery { x2y2Service.getNextEvents(EventType.CANCEL_LISTING, any()) } returns x2y2Event

        coEvery { orderStateRepository.getById(offChainEvent1.order.itemHash) } returns null
        coEvery { orderStateRepository.getById(offChainEvent2.order.itemHash) } returns null
        coEvery { orderStateRepository.getById(offChainEvent3.order.itemHash) } returns mockk()

        coEvery { orderStateRepository.save(any()) } returns mockk()

        coEvery { orderUpdateService.update(offChainEvent1.order.itemHash, any()) } returns createSellOrder()
        coEvery { orderUpdateService.update(offChainEvent2.order.itemHash, any()) } returns createSellOrder()

        handler.load(null)

        coVerify(exactly = 1) { x2y2Service.getNextEvents(EventType.CANCEL_LISTING, null) }

        coVerify(exactly = 1) {
            orderStateRepository.save(withArg {
                assertThat(it.id).isEqualTo(offChainEvent1.order.itemHash)
            })
            orderStateRepository.save(withArg {
                assertThat(it.id).isEqualTo(offChainEvent2.order.itemHash)
            })
        }
        coVerify(exactly = 1) {
            orderUpdateService.update(offChainEvent1.order.itemHash, any())
            orderUpdateService.update(offChainEvent2.order.itemHash, any())
        }
        coVerify(exactly = 2) { orderStateRepository.save(any()) }
        coVerify(exactly = 2) { orderUpdateService.update(any(), any()) }
    }
}
