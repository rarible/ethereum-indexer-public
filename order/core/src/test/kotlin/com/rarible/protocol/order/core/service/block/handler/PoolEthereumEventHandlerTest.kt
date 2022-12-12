package com.rarible.protocol.order.core.service.block.handler

import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.data.createLogEvent
import com.rarible.protocol.order.core.data.createOrderCancel
import com.rarible.protocol.order.core.data.createOrderSideMatch
import com.rarible.protocol.order.core.data.randomApproveHistory
import com.rarible.protocol.order.core.data.randomAuctionCreated
import com.rarible.protocol.order.core.data.randomPoolDeltaUpdate
import com.rarible.protocol.order.core.data.randomPoolFeeUpdate
import com.rarible.protocol.order.core.service.pool.listener.PoolOrderEventListener
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

internal class PoolEthereumEventHandlerTest {
    private val poolOrderEventListener = mockk<PoolOrderEventListener>()
    private val properties = mockk<OrderIndexerProperties.PoolEventHandleProperties>()

    private val handler = PoolEthereumEventHandler(poolOrderEventListener, properties)

    @Test
    fun `handle - all pool events`() = runBlocking<Unit> {
        val event1 = createLogEvent(randomPoolDeltaUpdate())
        val event2 = createLogEvent(randomPoolFeeUpdate())
        val others = listOf(
            createLogEvent(createOrderSideMatch()),
            createLogEvent(createOrderCancel()),
            createLogEvent(randomAuctionCreated()),
            createLogEvent(randomApproveHistory())
        )

        coEvery { poolOrderEventListener.onPoolEvent(event1) } returns Unit
        coEvery { poolOrderEventListener.onPoolEvent(event2) } returns Unit
        every { properties.parallel } returns false

        handler.handle(listOf(event1, event2) + others)

        coVerify(exactly = 1) {
            poolOrderEventListener.onPoolEvent(event1)
            poolOrderEventListener.onPoolEvent(event1)
        }
    }
}