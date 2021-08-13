package com.rarible.protocol.nftorder.listener.service

import com.rarible.protocol.dto.OrderDto
import com.rarible.protocol.dto.OrdersPaginationDto
import com.rarible.protocol.dto.PlatformDto
import com.rarible.protocol.nftorder.listener.configuration.NftOrderJobProperties
import com.rarible.protocol.nftorder.listener.configuration.OrderReconciliationConfig
import com.rarible.protocol.nftorder.listener.configuration.ReconciliationJobConfig
import com.rarible.protocol.order.api.client.OrderControllerApi
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono

internal class OrderReconciliationServiceTest {

    private val testPageSize = 10

    private val orderControllerApi: OrderControllerApi = mockk()
    private val orderEventService: OrderEventService = mockk()

    private val orderReconciliationService = OrderReconciliationService(
        orderControllerApi,
        orderEventService,
        NftOrderJobProperties(ReconciliationJobConfig(OrderReconciliationConfig(batchSize = testPageSize)))
    )

    @BeforeEach
    fun beforeEach() {
        clearMocks(orderControllerApi, orderEventService)
        coEvery { orderEventService.updateOrder(any()) } returns Unit
    }

    @Test
    fun `reconcile orders - first page`() = runBlocking {
        val nextContinuation = "1_1"
        mockGetOrdersAll(null, testPageSize, mockPagination(nextContinuation, testPageSize))

        val result = orderReconciliationService.reconcileOrders(null)

        assertEquals(nextContinuation, result)
        coVerify(exactly = testPageSize) { orderEventService.updateOrder(any()) }
    }

    @Test
    fun `reconcile orders - next page`() = runBlocking {
        val lastContinuation = "1_1"
        val nextContinuation = "2_2"
        mockGetOrdersAll(lastContinuation, testPageSize, mockPagination(nextContinuation, testPageSize))

        val result = orderReconciliationService.reconcileOrders(lastContinuation)

        assertEquals(nextContinuation, result)
        coVerify(exactly = testPageSize) { orderEventService.updateOrder(any()) }
    }

    @Test
    fun `reconcile orders - last page`() = runBlocking {
        val lastContinuation = "1_1"
        val nextContinuation = null
        mockGetOrdersAll(lastContinuation, testPageSize, mockPagination(nextContinuation, 10))

        val result = orderReconciliationService.reconcileOrders(lastContinuation)

        assertNull(result)
        coVerify(exactly = 10) { orderEventService.updateOrder(any()) }
    }

    @Test
    fun `reconcile orders - empty page`() = runBlocking {
        mockGetOrdersAll(null, testPageSize, mockPagination("1_1", 0))

        val result = orderReconciliationService.reconcileOrders(null)

        assertNull(result)
        coVerify(exactly = 0) { orderEventService.updateOrder(any()) }
    }

    private fun mockGetOrdersAll(continuation: String?, size: Int, result: OrdersPaginationDto): Unit {
        every {
            orderControllerApi.getOrdersAll(null, PlatformDto.ALL, continuation, size)
        } returns Mono.just(result)
    }

    private fun mockPagination(continuation: String?, count: Int): OrdersPaginationDto {
        val orders = ArrayList<OrderDto>()
        for (i in 1..count) {
            orders.add(mockk())
        }
        return OrdersPaginationDto(orders, continuation)
    }
}