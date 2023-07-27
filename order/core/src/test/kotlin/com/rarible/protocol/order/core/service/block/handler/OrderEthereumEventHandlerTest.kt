package com.rarible.protocol.order.core.service.block.handler

import com.rarible.core.test.data.randomWord
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.data.createLogRecordEvent
import com.rarible.protocol.order.core.data.createOrderCancel
import com.rarible.protocol.order.core.data.createOrderSideMatch
import com.rarible.protocol.order.core.data.createSellOrder
import com.rarible.protocol.order.core.data.randomApproveHistory
import com.rarible.protocol.order.core.data.randomAuctionCreated
import com.rarible.protocol.order.core.misc.asEthereumLogRecord
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

        val event1 = createLogRecordEvent(createOrderSideMatch().copy(hash = hash1))
        val event2 = createLogRecordEvent(createOrderCancel().copy(hash = hash1))
        val event3 = createLogRecordEvent(createOrderSideMatch().copy(hash = hash2))
        val event4 = createLogRecordEvent(createOrderCancel().copy(hash = hash2))
        val others = listOf(createLogRecordEvent(randomAuctionCreated()), createLogRecordEvent(randomApproveHistory()))

        coEvery { orderUpdateService.update(eq(hash1), any()) } returns createSellOrder()
        coEvery { orderUpdateService.update(eq(hash2), any()) } returns createSellOrder()
        every { eventFilter.filter(any()) } returns true
        every { properties.parallel } returns false

        handler.handle(listOf(event1, event2, event3, event4) + others)

        coVerify(exactly = 1) {
            orderUpdateService.update(eq(hash1), any())
            orderUpdateService.update(eq(hash2), any())
        }
    }

    @Test
    fun `filter unneeded events`() = runBlocking<Unit> {
        val hash1 = Word.apply(randomWord())
        val hash2 = Word.apply(randomWord())

        val event1 = createLogRecordEvent(createOrderSideMatch().copy(hash = hash1))
        val event2 = createLogRecordEvent(createOrderSideMatch().copy(hash = hash2))

        coEvery { orderUpdateService.update(any(), any()) } returns createSellOrder()
        every { eventFilter.filter(event1.record.asEthereumLogRecord()) } returns true
        every { eventFilter.filter(event2.record.asEthereumLogRecord()) } returns false
        every { properties.parallel } returns true
        every { properties.chunkSize } returns 10

        handler.handle(listOf(event1, event2))

        coVerify(exactly = 1) { orderUpdateService.update(eq(hash1), any()) }
        verify { properties.chunkSize }
        verify { properties.parallel }
    }
}
