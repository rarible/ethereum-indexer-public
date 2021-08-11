package com.rarible.protocol.nftorder.listener.handler

import com.rarible.protocol.nftorder.listener.service.OrderReconciliationService
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class OrderReconciliationTaskHandlerTest {

    private val orderReconciliationService: OrderReconciliationService = mockk()
    private val orderReconciliationTaskHandler = OrderReconciliationTaskHandler(orderReconciliationService)

    @BeforeEach
    fun beforeEach() {
        clearMocks(orderReconciliationService)
    }

    @Test
    fun `run reconciliation task`() = runBlocking {
        coEvery { orderReconciliationService.reconcileOrders(null) } returns "1_1"
        coEvery { orderReconciliationService.reconcileOrders("1_1") } returns "2_2"
        coEvery { orderReconciliationService.reconcileOrders("2_2") } returns null

        val result = orderReconciliationTaskHandler.runLongTask(null, "").toList()

        assertEquals(2, result.size)
        assertEquals("1_1", result[0])
        assertEquals("2_2", result[1])

        coVerify(exactly = 1) { orderReconciliationService.reconcileOrders(null) }
        coVerify(exactly = 1) { orderReconciliationService.reconcileOrders("1_1") }
        coVerify(exactly = 1) { orderReconciliationService.reconcileOrders("2_2") }
    }

}