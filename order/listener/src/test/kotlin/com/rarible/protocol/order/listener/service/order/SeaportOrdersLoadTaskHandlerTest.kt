package com.rarible.protocol.order.listener.service.order

import com.rarible.core.test.data.randomString
import com.rarible.opensea.client.model.v2.SeaportOrders
import com.rarible.protocol.order.listener.data.randomSeaportOrder
import com.rarible.protocol.order.listener.service.opensea.SeaportOrderLoader
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

internal class SeaportOrdersLoadTaskHandlerTest {
    private val loader = mockk<SeaportOrderLoader>()
    private val task = SeaportOrdersLoadTaskHandler(loader)

    @Test
    fun `should load all orders`() = runBlocking<Unit> {
        val cursor1 = randomString()
        val cursor2 = randomString()
        val result1 = SeaportOrders(next = cursor2, orders = listOf(randomSeaportOrder()), previous = null)
        val result2 = SeaportOrders(next = null, orders = listOf(randomSeaportOrder()), previous = null)

        coEvery { loader.load(cursor1, false, any()) } returns result1
        coEvery { loader.load(cursor2, false, any()) } returns result2

        val states = task.runLongTask(cursor1, "").toList()
        assertThat(states.single()).isEqualTo(cursor2)
    }

    @Test
    fun `should stop load if no orders`() = runBlocking<Unit> {
        val cursor = randomString()
        val nexCursor = randomString()
        val result = SeaportOrders(next = nexCursor, orders = emptyList(), previous = null)

        coEvery { loader.load(cursor, false, any()) } returns result

        val states = task.runLongTask(cursor, "").toList()
        assertThat(states.single()).isEqualTo(nexCursor)
    }

    @Test
    fun `should stop load if load all listed after`() = runBlocking<Unit> {
        val cursor1 = randomString()
        val cursor2 = randomString()
        val cursor3 = randomString()
        val order1 = listOf(
            randomSeaportOrder().copy(createdAt = Instant.ofEpochSecond(20)),
            randomSeaportOrder().copy(createdAt = Instant.ofEpochSecond(19)),
        )
        val order2 = listOf(
            randomSeaportOrder().copy(createdAt = Instant.ofEpochSecond(10)),
            randomSeaportOrder().copy(createdAt = Instant.ofEpochSecond(9)),
        )
        val result1 = SeaportOrders(next = cursor2, orders = order1, previous = null)
        val result2 = SeaportOrders(next = cursor3, orders = order2, previous = null)

        coEvery { loader.load(cursor1, false, any()) } returns result1
        coEvery { loader.load(cursor2, false, any()) } returns result2

        val listedAfter = Instant.ofEpochSecond(9)
        val states = task.runLongTask(cursor1, listedAfter.epochSecond.toString()).toList()
        assertThat(states).hasSize(2)
        assertThat(states.last()).isEqualTo(cursor3)
    }
}
