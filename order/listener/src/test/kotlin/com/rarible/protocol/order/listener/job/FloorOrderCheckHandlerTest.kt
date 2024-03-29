package com.rarible.protocol.order.listener.job

import com.rarible.core.test.data.randomAddress
import com.rarible.protocol.order.core.data.randomOrder
import com.rarible.protocol.order.core.exception.OrderDataException
import com.rarible.protocol.order.core.metric.FloorOrderCheckMetrics
import com.rarible.protocol.order.core.model.order.OrderSimulation
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.validator.OrderValidator
import com.rarible.protocol.order.listener.service.order.OrderSimulationService
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class FloorOrderCheckHandlerTest {

    @MockK
    lateinit var orderRepository: OrderRepository

    @MockK
    lateinit var coreOrderValidator: OrderValidator

    @MockK
    lateinit var orderSimulationService: OrderSimulationService

    @MockK
    lateinit var topCollectionProvider: TopCollectionProvider

    @InjectMockKs
    lateinit var handler: FloorOrderCheckHandler

    @MockK
    lateinit var metrics: FloorOrderCheckMetrics

    @BeforeEach
    fun beforeEach() {
        clearMocks(topCollectionProvider, coreOrderValidator, orderRepository)
        every { metrics.onOrderChecked() } returns Unit
        every { metrics.onOrderSimulated(any()) } returns Unit
    }

    @Test
    fun `handle - ok`() = runBlocking<Unit> {
        every { orderSimulationService.isEnabled } returns false

        val collection = randomAddress()

        val currency1 = randomAddress()
        val currency2 = randomAddress()

        coEvery { topCollectionProvider.getTopCollections() } returns listOf(collection)

        coEvery {
            orderRepository.findActiveSellCurrenciesByCollection(collection)
        } returns listOf(currency1, currency2)

        val invalidOrder1 = randomOrder()
        val validOrder1 = randomOrder()
        val validOrder2 = randomOrder()

        coEvery {
            orderRepository.findActiveBestSellOrdersOfCollection(collection, currency1, 1)
        }.returnsMany(listOf(invalidOrder1), listOf(validOrder1))

        coEvery {
            orderRepository.findActiveBestSellOrdersOfCollection(collection, currency2, 1)
        }.returnsMany(listOf(validOrder2))

        coEvery { coreOrderValidator.validate(invalidOrder1) } throws OrderDataException("failed")
        coEvery { coreOrderValidator.validate(validOrder1) } returns Unit
        coEvery { coreOrderValidator.validate(validOrder2) } returns Unit

        handler.handle()

        coVerify(exactly = 2) { orderRepository.findActiveBestSellOrdersOfCollection(collection, currency1, 1) }
        coVerify(exactly = 1) { orderRepository.findActiveBestSellOrdersOfCollection(collection, currency2, 1) }

        coVerify(exactly = 1) { coreOrderValidator.validate(invalidOrder1) }
        coVerify(exactly = 1) { coreOrderValidator.validate(validOrder1) }
        coVerify(exactly = 1) { coreOrderValidator.validate(validOrder2) }
        coVerify(exactly = 0) { orderSimulationService.simulate(any()) }
    }

    @Test
    fun `simulate - ok`() = runBlocking<Unit> {
        every { orderSimulationService.isEnabled } returns true
        coEvery { orderSimulationService.simulate(any()) } returns OrderSimulation.SUCCESS

        val collection = randomAddress()

        val currency = randomAddress()

        coEvery { topCollectionProvider.getTopCollections() } returns listOf(collection)

        coEvery {
            orderRepository.findActiveSellCurrenciesByCollection(collection)
        } returns listOf(currency)

        val validOrder1 = randomOrder()

        coEvery {
            orderRepository.findActiveBestSellOrdersOfCollection(collection, currency, 1)
        }.returnsMany(listOf(validOrder1))

        coEvery { coreOrderValidator.validate(validOrder1) } returns Unit

        handler.handle()

        coVerify(exactly = 1) { orderSimulationService.simulate(any()) }
    }

    @Test
    fun `handle - ok, empty top collection list`() = runBlocking<Unit> {
        every { orderSimulationService.isEnabled } returns false
        coEvery { topCollectionProvider.getTopCollections() } returns listOf()

        handler.handle()

        coVerify(exactly = 0) { orderRepository.findActiveSellCurrenciesByCollection(any()) }
        coVerify(exactly = 0) { orderRepository.findActiveBestSellOrdersOfCollection(any(), any(), any()) }
        coVerify(exactly = 0) { coreOrderValidator.validate(any()) }
    }

    @Test
    fun `handle - ok, no currencies found`() = runBlocking<Unit> {
        every { orderSimulationService.isEnabled } returns false
        val collection = randomAddress()

        coEvery { topCollectionProvider.getTopCollections() } returns listOf(collection)
        coEvery { orderRepository.findActiveSellCurrenciesByCollection(collection) } returns listOf()

        handler.handle()

        coVerify(exactly = 1) { orderRepository.findActiveSellCurrenciesByCollection(collection) }
        coVerify(exactly = 0) { orderRepository.findActiveBestSellOrdersOfCollection(any(), any(), any()) }
        coVerify(exactly = 0) { coreOrderValidator.validate(any()) }
    }

    @Test
    fun `handle - ok, no orders found`() = runBlocking<Unit> {
        every { orderSimulationService.isEnabled } returns false
        val collection = randomAddress()
        val currency = randomAddress()

        coEvery { topCollectionProvider.getTopCollections() } returns listOf(collection)
        coEvery { orderRepository.findActiveSellCurrenciesByCollection(collection) } returns listOf(currency)
        coEvery { orderRepository.findActiveBestSellOrdersOfCollection(collection, currency, 1) } returns emptyList()

        handler.handle()

        coVerify(exactly = 1) { orderRepository.findActiveSellCurrenciesByCollection(collection) }
        coVerify(exactly = 1) { orderRepository.findActiveBestSellOrdersOfCollection(collection, currency, 1) }
        coVerify(exactly = 0) { coreOrderValidator.validate(any()) }
    }
}
