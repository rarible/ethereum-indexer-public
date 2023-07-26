package com.rarible.protocol.order.core.service.x2y2

import com.rarible.protocol.order.listener.data.randomX2Y2Event
import com.rarible.protocol.order.listener.data.randomX2Y2Order
import com.rarible.x2y2.client.X2Y2ApiClient
import com.rarible.x2y2.client.model.ApiListResponse
import com.rarible.x2y2.client.model.EventType
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class X2Y2ServiceTest {

    private val x2y2ApiClient = mockk<X2Y2ApiClient>()
    private val service = X2Y2Service(
        x2y2ApiClient
    )

    @Test
    fun testSuccessResul() = runBlocking<Unit> {
        val cursor = "test"
        val orders = listOf(randomX2Y2Order(), randomX2Y2Order())
        val expectedResult = ApiListResponse(success = true, data = orders, next = null)
        coEvery { x2y2ApiClient.orders(cursor = cursor) } returns expectedResult

        val result = service.getNextSellOrders(cursor)
        assertThat(result).isEqualTo(expectedResult)
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
