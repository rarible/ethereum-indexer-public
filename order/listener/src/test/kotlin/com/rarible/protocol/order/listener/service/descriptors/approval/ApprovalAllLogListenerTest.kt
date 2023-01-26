package com.rarible.protocol.order.listener.service.descriptors.approval

import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.data.createLogEvent
import com.rarible.protocol.order.core.data.createOrder
import com.rarible.protocol.order.core.data.randomApproveHistory
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.core.service.approve.ApproveService
import com.rarible.protocol.order.core.service.block.approval.ApprovalAllLogListener
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

internal class ApprovalAllLogListenerTest {
    private val orderRepository = mockk<OrderRepository>()
    private val orderUpdateService = mockk<OrderUpdateService>()
    private val approveService = mockk<ApproveService>()
    private val properties = mockk<OrderIndexerProperties>()

    private val logListener = ApprovalAllLogListener(
        orderRepository = orderRepository,
        orderUpdateService = orderUpdateService,
        approveService = approveService,
        properties =  properties
    )

    @Test
    fun `process approval - ok`() = runBlocking<Unit> {
        val approval = randomApproveHistory()
        val logEvent = createLogEvent(approval).copy(blockNumber = 10)
        val platform = Platform.RARIBLE
        val foundOrder1 = createOrder()
        val foundOrder2 = createOrder()

        every { properties.handleApprovalAfterBlock } returns 5
        every { approveService.getPlatform(approval.operator) } returns platform
        every { orderRepository.findActiveSaleOrdersHashesByMakerAndToken(
            approval.owner,
            approval.collection,
            platform
        ) } returns flow { listOf(foundOrder1, foundOrder2).forEach { emit(it) } }
        coEvery {
            orderUpdateService.updateApproval(eq(foundOrder1), eq(approval.approved), any())
            orderUpdateService.updateApproval(eq(foundOrder2), eq(approval.approved), any())
        } returns Unit

        logListener.onLogEvent(logEvent).awaitFirstOrNull()
        coVerify(exactly = 2) { orderUpdateService.updateApproval(any(), eq(approval.approved), any()) }
    }

    @Test
    fun `process approval - skip`() = runBlocking<Unit> {
        val approval = randomApproveHistory()
        val logEvent = createLogEvent(approval).copy(blockNumber = 10)

        every { properties.handleApprovalAfterBlock } returns 15
        logListener.onLogEvent(logEvent).awaitFirstOrNull()
        coVerify(exactly = 0) { orderUpdateService.updateApproval(any(), any(), any()) }
    }
}