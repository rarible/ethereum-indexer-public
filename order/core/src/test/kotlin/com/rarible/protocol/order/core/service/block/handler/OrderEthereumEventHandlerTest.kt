package com.rarible.protocol.order.core.service.block.handler

import com.rarible.core.test.data.randomWord
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.data.createLogEvent
import com.rarible.protocol.order.core.data.createOrderCancel
import com.rarible.protocol.order.core.data.createOrderSideMatch
import com.rarible.protocol.order.core.data.randomApproveHistory
import com.rarible.protocol.order.core.data.randomAuctionCreated
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.core.service.block.filter.EthereumEventFilter
import io.daonomic.rpc.domain.Word
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

internal class OrderEthereumEventHandlerTest {
    private val orderUpdateService = mockk<OrderUpdateService>()
    private val eventFilter = mockk<EthereumEventFilter>()
    private val properties = mockk<OrderIndexerProperties.OrderEventHandleProperties>()

    private val handler = OrderEthereumEventHandler(orderUpdateService, listOf(eventFilter), properties)

    @Test
    fun `handle - all unique order events`() = runBlocking<Unit> {
        val hash1 = Word.apply(randomWord())
        val hash2 = Word.apply(randomWord())

        val event1 = createLogEvent(createOrderSideMatch().copy(hash = hash1))
        val event2 = createLogEvent(createOrderCancel().copy(hash = hash1))
        val event3 = createLogEvent(createOrderSideMatch().copy(hash = hash2))
        val event4 = createLogEvent(createOrderCancel().copy(hash = hash2))
        val others = listOf(createLogEvent(randomAuctionCreated()), createLogEvent(randomApproveHistory()))

        coEvery { orderUpdateService.update(hash1) } returns Unit
        coEvery { orderUpdateService.update(hash2) } returns Unit
        every { eventFilter.filter(any()) } returns true
        every { properties.parallel } returns false

        handler.handle(listOf(event1, event2, event3, event4) + others)

        coVerify(exactly = 1) {
            orderUpdateService.update(hash1)
            orderUpdateService.update(hash2)
        }
    }

    @Test
    fun `filter unneeded events`() = runBlocking<Unit> {
        val hash1 = Word.apply(randomWord())
        val hash2 = Word.apply(randomWord())

        val event1 = createLogEvent(createOrderSideMatch().copy(hash = hash1))
        val event2 = createLogEvent(createOrderSideMatch().copy(hash = hash2))

        coEvery { orderUpdateService.update(any()) } returns Unit
        every { eventFilter.filter(event1) } returns true
        every { eventFilter.filter(event2) } returns false
        every { properties.parallel } returns true
        every { properties.chunkSize } returns 10

        handler.handle(listOf(event1, event2))

        coVerify(exactly = 1) { orderUpdateService.update(hash1) }
        verify { properties.chunkSize }
        verify { properties.parallel }
    }
}