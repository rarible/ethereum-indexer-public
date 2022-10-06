package com.rarible.protocol.order.listener.service.x2y2

import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.core.telemetry.metrics.RegisteredGauge
import com.rarible.protocol.order.listener.data.randomX2Y2Event
import com.rarible.protocol.order.listener.data.randomX2Y2Order
import com.rarible.x2y2.client.X2Y2ApiClient
import com.rarible.x2y2.client.model.ApiListResponse
import com.rarible.x2y2.client.model.EventType
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class X2Y2ServiceTest {
    private val x2y2ApiClient = mockk<X2Y2ApiClient>()

    private val x2y2OrderDelayGauge = mockk<RegisteredGauge<Long>> {
        every { set(any()) } returns Unit
    }
    private val x2y2LoadCounter = mockk<RegisteredCounter> {
        every { increment(any()) } returns Unit
    }
    private val x2y2EventDelayGauge = mockk<RegisteredGauge<Long>> {
        every { set(any()) } returns Unit
    }
    private val x2y2EventLoadCounter = mockk<RegisteredCounter> {
        every { increment(any()) } returns Unit
    }
    private val service = X2Y2Service(
        x2y2ApiClient,
        x2y2OrderDelayGauge,
        x2y2LoadCounter,
        x2y2EventLoadCounter,
        x2y2EventDelayGauge
    )

    @Test
    fun testSuccessResul() = runBlocking<Unit> {
        val cursor = "test"
        val orders = listOf(randomX2Y2Order(), randomX2Y2Order())
        val expectedResult = ApiListResponse(success = true, data = orders, next = null)
        coEvery { x2y2ApiClient.orders(cursor = cursor) } returns expectedResult

        val result = service.getNextSellOrders(cursor)
        assertThat(result).isEqualTo(expectedResult)
        verify { x2y2OrderDelayGauge.set(any()) }
        verify { x2y2LoadCounter.increment(orders.size) }
    }

    @Test
    fun testSuccessEventResul() = runBlocking<Unit> {
        val cursor = "test"
        val events = listOf(randomX2Y2Event(), randomX2Y2Event())
        val expectedResult = ApiListResponse(success = true, data = events, next = null)
        val type = EventType.CANCEL_LISTING
        coEvery { x2y2ApiClient.events(type = type, cursor = cursor) } returns expectedResult

        val result = service.getNextEvents(type, cursor)
        assertThat(result).isEqualTo(expectedResult)
        verify { x2y2EventDelayGauge.set(any()) }
        verify { x2y2EventLoadCounter.increment(events.size) }
    }

    @Test
    fun testFailResul() = runBlocking<Unit> {
        val cursor = "test"
        coEvery { x2y2ApiClient.orders(cursor = cursor) } returns ApiListResponse(success = false, data = emptyList(), next = null)
        assertThrows<IllegalStateException> {
            runBlocking {
                service.getNextSellOrders(cursor)
            }
        }
    }
}