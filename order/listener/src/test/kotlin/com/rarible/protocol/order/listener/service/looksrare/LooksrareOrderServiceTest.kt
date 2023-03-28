package com.rarible.protocol.order.listener.service.looksrare

import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.looksrare.client.LooksrareClient
import com.rarible.looksrare.client.model.LooksrareResult
import com.rarible.looksrare.client.model.v1.LooksrareOrders
import com.rarible.looksrare.client.model.v1.Sort
import com.rarible.looksrare.client.model.v1.Status
import com.rarible.protocol.order.listener.configuration.LooksrareLoadProperties
import com.rarible.protocol.order.listener.data.randomLooksrareOrder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

internal class LooksrareOrderServiceTest {
    private val looksrareClient = mockk<LooksrareClient>()
    private val looksrareLoadCounter = mockk<RegisteredCounter> {
        every { increment(any()) } returns Unit
    }
    private val properties = LooksrareLoadProperties()

    private val service = LooksrareOrderService(
        looksrareClient,
        looksrareLoadCounter,
        properties
    )

    @Test
    fun `should get all order in one iteration`() = runBlocking<Unit> {
        val listedBefore = Instant.now()
        val listedAfter = listedBefore - Duration.ofSeconds(10)

        val order1 = randomLooksrareOrder().copy(status = Status.VALID)
        val order2 = randomLooksrareOrder().copy(startTime = listedAfter - Duration.ofSeconds(1), status = Status.VALID)

        val result = LooksrareOrders(success = true, message = "", data = listOf(order1, order2))
        coEvery { looksrareClient.getOrders(any()) } returns LooksrareResult.success(result)

        val orders = service.getNextSellOrders(listedAfter, listedBefore)
        assertThat(orders).containsExactly(order1, order2)

        coVerify(exactly = 1) {
            looksrareClient.getOrders(
                match {
                    it.isOrderAsk
                    it.startTime == listedBefore
                    it.endTime == null
                    it.status == null
                    it.sort == null
                    it.pagination?.first == properties.loadMaxSize
                    it.pagination?.cursor == null
                }
            )
        }
    }

    @Test
    fun `should get all order in two iterations`() = runBlocking<Unit> {
        val listedBefore = Instant.now()
        val listedAfter = listedBefore - Duration.ofSeconds(10)

        val order1 = randomLooksrareOrder().copy(status = Status.VALID)
        val order2 = randomLooksrareOrder().copy(startTime = listedAfter + Duration.ofSeconds(1), status = Status.VALID)
        val order3 = randomLooksrareOrder().copy(status = Status.VALID)
        val order4 = randomLooksrareOrder().copy(startTime = listedAfter - Duration.ofSeconds(1), status = Status.VALID)

        val result1 = LooksrareOrders(success = true, message = "", data = listOf(order1, order2))
        val result2 = LooksrareOrders(success = true, message = "", data = listOf(order3, order4))
        coEvery { looksrareClient.getOrders(any()) } returnsMany listOf(LooksrareResult.success(result1), LooksrareResult.success(result2))

        val orders = service.getNextSellOrders(listedAfter, listedBefore)
        assertThat(orders).containsExactly(order1, order2, order3, order4)

        coVerify(exactly = 1) {
            looksrareClient.getOrders(
                match { it.pagination?.cursor == null }
            )
        }
        coVerify(exactly = 1) {
            looksrareClient.getOrders(
                match { it.pagination?.cursor == order2.hash.prefixed() }
            )
        }
    }
}
