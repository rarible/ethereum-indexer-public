package com.rarible.protocol.order.listener.service.looksrare

import com.rarible.looksrare.client.LooksrareClientV2
import com.rarible.looksrare.client.model.LooksrareResult
import com.rarible.looksrare.client.model.v2.LooksrareOrders
import com.rarible.looksrare.client.model.v2.Sort
import com.rarible.looksrare.client.model.v2.Status
import com.rarible.protocol.order.core.model.LooksrareV2Cursor
import com.rarible.protocol.order.listener.configuration.LooksrareLoadProperties
import com.rarible.protocol.order.listener.data.randomLooksrareOrder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

internal class LooksrareOrderServiceTest {
    private val looksrareClient = mockk<LooksrareClientV2>()
    private val properties = LooksrareLoadProperties()

    private val service = LooksrareOrderService(
        looksrareClient,
        properties
    )

    @Test
    fun `should get all order in one iteration`() = runBlocking<Unit> {
        val createdAfter = LooksrareV2Cursor(Instant.now() - Duration.ofHours(1))

        val order1 = randomLooksrareOrder().copy(createdAt = createdAfter.createdAfter, status = Status.VALID)
        val order2 = randomLooksrareOrder().copy(createdAt = createdAfter.createdAfter - Duration.ofHours(2), status = Status.VALID)

        val result = LooksrareOrders(success = true, message = "", data = listOf(order1, order2))
        coEvery { looksrareClient.getOrders(any()) } returns LooksrareResult.success(result)

        val orders = service.getNextSellOrders(createdAfter)
        assertThat(orders).containsExactly(order1, order2)

        coVerify(exactly = 1) {
            looksrareClient.getOrders(
                match {
                    it.status == Status.VALID &&
                    it.sort == Sort.NEWEST &&
                    it.pagination?.first == properties.loadMaxSize &&
                    it.pagination?.cursor == null
                }
            )
        }
    }

    @Test
    fun `should get all order in two iterations`() = runBlocking<Unit> {
        val now = Instant.now()
        val createdAfter = LooksrareV2Cursor(now - Duration.ofSeconds(10))

        val order1 = randomLooksrareOrder().copy(status = Status.VALID, createdAt = now)
        val order2 = randomLooksrareOrder().copy(status = Status.VALID, createdAt = now - Duration.ofSeconds(1))
        val order3 = randomLooksrareOrder().copy(status = Status.VALID, createdAt = now - Duration.ofSeconds(2))
        val order4 = randomLooksrareOrder().copy(status = Status.VALID, createdAt = now - Duration.ofSeconds(11))

        val result1 = LooksrareOrders(success = true, message = "", data = listOf(order1, order2))
        val result2 = LooksrareOrders(success = true, message = "", data = listOf(order3, order4))
        coEvery { looksrareClient.getOrders(any()) } returnsMany listOf(LooksrareResult.success(result1), LooksrareResult.success(result2))

        val orders = service.getNextSellOrders(createdAfter)
        assertThat(orders).containsExactly(order1, order2, order3, order4)

        coVerify(exactly = 1) {
            looksrareClient.getOrders(
                match { it.pagination?.cursor == null }
            )
        }
        coVerify(exactly = 1) {
            looksrareClient.getOrders(
                match { it.pagination?.cursor == order2.id }
            )
        }
    }
}
