package com.rarible.protocol.order.core.service.block.handler

import com.rarible.core.test.data.randomWord
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.data.createLogRecordEvent
import com.rarible.protocol.order.core.data.createOrderCancel
import com.rarible.protocol.order.core.data.createOrderSideMatch
import com.rarible.protocol.order.core.data.createSellOrder
import com.rarible.protocol.order.core.data.randomApproveHistory
import com.rarible.protocol.order.core.data.randomAuctionCreated
import com.rarible.protocol.order.core.data.randomPoolDeltaUpdate
import com.rarible.protocol.order.core.data.randomPoolFeeUpdate
import com.rarible.protocol.order.core.misc.asEthereumLogRecord
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.core.service.pool.listener.PoolOrderEventListener
import io.daonomic.rpc.domain.Word
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

internal class PoolEthereumEventHandlerTest {
    private val poolOrderEventListener = mockk<PoolOrderEventListener>()
    private val orderUpdateService = mockk<OrderUpdateService>()
    private val properties = mockk<OrderIndexerProperties.PoolEventHandleProperties>()
    private val handler = PoolEthereumEventHandler(poolOrderEventListener, orderUpdateService, properties)

    @Test
    fun `handle - all pool events`() = runBlocking<Unit> {
        val hash1 = Word.apply(randomWord())
        val hash2 = Word.apply(randomWord())

        val event1 = createLogRecordEvent(randomPoolDeltaUpdate().copy(hash = hash1))
        val event2 = createLogRecordEvent(randomPoolFeeUpdate().copy(hash = hash1))
        val event3 = createLogRecordEvent(randomPoolDeltaUpdate().copy(hash = hash2))
        val event4 = createLogRecordEvent(randomPoolFeeUpdate().copy(hash = hash2))

        val others = listOf(
            createLogRecordEvent(createOrderSideMatch()),
            createLogRecordEvent(createOrderCancel()),
            createLogRecordEvent(randomAuctionCreated()),
            createLogRecordEvent(randomApproveHistory())
        )
        coEvery {
            poolOrderEventListener.onPoolEvent(event1.record.asEthereumLogRecord(), any())
            poolOrderEventListener.onPoolEvent(event2.record.asEthereumLogRecord(), any())
            poolOrderEventListener.onPoolEvent(event3.record.asEthereumLogRecord(), any())
            poolOrderEventListener.onPoolEvent(event4.record.asEthereumLogRecord(), any())
        } returns Unit
        coEvery {
            orderUpdateService.update(eq(hash1), any())
            orderUpdateService.update(eq(hash2), any())
        } returns createSellOrder()

        every { properties.parallel } returns false

        handler.handle(listOf(event1, event2, event3, event4) + others)

        coVerify(exactly = 1) {
            poolOrderEventListener.onPoolEvent(event1.record.asEthereumLogRecord(), any())
            poolOrderEventListener.onPoolEvent(event2.record.asEthereumLogRecord(), any())
            poolOrderEventListener.onPoolEvent(event3.record.asEthereumLogRecord(), any())
            poolOrderEventListener.onPoolEvent(event4.record.asEthereumLogRecord(), any())
        }
        coVerify(exactly = 1) {
            orderUpdateService.update(eq(hash1), any())
            orderUpdateService.update(eq(hash2), any())
        }
        coVerifyOrder {
            orderUpdateService.update(eq(hash1), any())
            poolOrderEventListener.onPoolEvent(event1.record.asEthereumLogRecord(), any())
            poolOrderEventListener.onPoolEvent(event2.record.asEthereumLogRecord(), any())
        }
        coVerifyOrder {
            orderUpdateService.update(eq(hash2), any())
            poolOrderEventListener.onPoolEvent(event3.record.asEthereumLogRecord(), any())
            poolOrderEventListener.onPoolEvent(event4.record.asEthereumLogRecord(), any())
        }
    }
}
