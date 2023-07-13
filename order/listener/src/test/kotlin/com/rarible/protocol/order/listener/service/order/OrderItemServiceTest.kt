package com.rarible.protocol.order.listener.service.order

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.data.createNftItemDto
import com.rarible.protocol.order.core.data.randomOrder
import com.rarible.protocol.order.core.misc.orderStubEventMarks
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderCancelService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class OrderItemServiceTest {

    @MockK
    lateinit var orderRepository: OrderRepository

    @MockK
    lateinit var orderCancelService: OrderCancelService

    @InjectMockKs
    lateinit var orderItemService: OrderItemService

    @Test
    fun `on item changed - ok, suspicious`() = runBlocking<Unit> {
        val item = createNftItemDto().copy(isSuspiciousOnOS = true)
        val order = randomOrder()

        coEvery {
            orderRepository.findSellOrdersNotCancelledByItemId(
                Platform.OPEN_SEA,
                item.contract,
                EthUInt256.of(item.tokenId)
            )
        } returns flowOf(order)

        coEvery { orderCancelService.cancelOrder(order.hash, any()) } returns Unit

        orderItemService.onItemChanged(item, orderStubEventMarks())

        coVerify(exactly = 1) { orderCancelService.cancelOrder(order.hash, any()) }
    }

    @Test
    fun `on item changed - skipped, not suspicious`() = runBlocking<Unit> {
        val item = createNftItemDto()

        orderItemService.onItemChanged(item, orderStubEventMarks())

        coVerify(exactly = 0) { orderCancelService.cancelOrder(any(), any()) }
    }

}