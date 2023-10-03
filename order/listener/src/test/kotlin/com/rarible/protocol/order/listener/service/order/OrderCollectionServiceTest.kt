package com.rarible.protocol.order.listener.service.order

import com.rarible.core.test.data.randomAddress
import com.rarible.protocol.dto.NftCollectionFlagDto
import com.rarible.protocol.order.core.data.createBidOrder
import com.rarible.protocol.order.core.data.createNftCollectionDto
import com.rarible.protocol.order.core.data.createSellOrder
import com.rarible.protocol.order.core.misc.orderStubEventMarks
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderCancelService
import io.mockk.called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class OrderCollectionServiceTest {

    @MockK
    lateinit var orderRepository: OrderRepository

    @MockK
    lateinit var orderCancelService: OrderCancelService

    @InjectMockKs
    lateinit var orderCollectionService: OrderCollectionService

    @Test
    fun `paused collection cancel orders`() = runBlocking<Unit> {
        val token = randomAddress()
        val sellOrder = createSellOrder()
        val buyOrder = createBidOrder()
        val collection = createNftCollectionDto(token)
            .copy(flagsTyped = listOf(NftCollectionFlagDto(NftCollectionFlagDto.Flag.PAUSED, "true")))

        coEvery {
            orderRepository.findNotCancelledOrdersByToken(token)
        } returns flowOf(sellOrder, buyOrder)
        coEvery { orderCancelService.cancelOrder(any(), any()) } returns createSellOrder()

        orderCollectionService.onCollectionChanged(collection, orderStubEventMarks())

        coVerify(exactly = 1) {
            orderCancelService.cancelOrder(sellOrder.hash, any())
            orderCancelService.cancelOrder(buyOrder.hash, any())
        }
    }

    @Test
    fun `unpaused collection do nothing`() = runBlocking<Unit> {
        val token = randomAddress()
        val sellOrder = createSellOrder()
        val buyOrder = createBidOrder()
        val collection = createNftCollectionDto(token)
            .copy(flagsTyped = listOf(NftCollectionFlagDto(NftCollectionFlagDto.Flag.PAUSED, "false")))

        coEvery {
            orderRepository.findNotCancelledOrdersByToken(token)
        } returns flowOf(sellOrder, buyOrder)
        coEvery { orderCancelService.cancelOrder(any(), any()) } returns createSellOrder()

        orderCollectionService.onCollectionChanged(collection, orderStubEventMarks())

        verify { orderCancelService wasNot called }
    }
}
