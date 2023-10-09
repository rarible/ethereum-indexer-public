package com.rarible.protocol.order.listener.service.order

import com.rarible.core.test.data.randomAddress
import com.rarible.protocol.dto.EthCollectionFlagsDto
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.data.createBidOrder
import com.rarible.protocol.order.core.data.createNftCollectionDto
import com.rarible.protocol.order.core.data.createSellOrder
import com.rarible.protocol.order.core.misc.orderStubEventMarks
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderCancelService
import io.mockk.called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class OrderCollectionServiceTest {

    @MockK
    lateinit var orderRepository: OrderRepository

    @MockK
    lateinit var orderCancelService: OrderCancelService

    lateinit var orderCollectionService: OrderCollectionService

    @BeforeEach
    fun setUp() {
        orderCollectionService =
            OrderCollectionService(orderRepository, orderCancelService, OrderIndexerProperties.FeatureFlags())
    }

    @Test
    fun `paused collection cancel orders`() = runBlocking<Unit> {
        val token = randomAddress()
        val sellOrder = createSellOrder()
        val buyOrder = createBidOrder()
        val collection = createNftCollectionDto(token)
            .copy(flags = EthCollectionFlagsDto(paused = true))

        coEvery {
            orderRepository.findNonTerminateOrdersByToken(token)
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
            .copy(flags = EthCollectionFlagsDto(paused = false))

        coEvery {
            orderRepository.findNonTerminateOrdersByToken(token)
        } returns flowOf(sellOrder, buyOrder)
        coEvery { orderCancelService.cancelOrder(any(), any()) } returns createSellOrder()

        orderCollectionService.onCollectionChanged(collection, orderStubEventMarks())

        verify { orderCancelService wasNot called }
    }
}
