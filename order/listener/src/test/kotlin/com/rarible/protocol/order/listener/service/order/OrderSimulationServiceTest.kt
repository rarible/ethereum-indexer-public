package com.rarible.protocol.order.listener.service.order

import com.rarible.protocol.order.core.data.randomOrder
import com.rarible.protocol.order.core.model.BuyTx
import com.rarible.protocol.order.core.model.order.OrderSimulation
import com.rarible.protocol.order.listener.configuration.FloorOrderCheckWorkerProperties
import com.rarible.protocol.order.listener.service.tenderly.SimulationResult
import com.rarible.protocol.order.listener.service.tenderly.TenderlyService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OrderSimulationServiceTest {

    val properties: FloorOrderCheckWorkerProperties = FloorOrderCheckWorkerProperties(
        simulateFloorOrders = true
    )
    val transactionService: OrderTransactionService = mockk()
    val tenderlyService: TenderlyService = mockk()

    val service = OrderSimulationService(properties, transactionService, tenderlyService)

    @Test
    fun `simulation - ok`() = runBlocking<Unit> {
        val order = randomOrder()
        val buyTx: BuyTx = mockk()
        val simulationResult = SimulationResult(status = true)
        coEvery { transactionService.buyTx(any(), any()) } returns buyTx
        coEvery { tenderlyService.simulate(buyTx) } returns simulationResult
        coEvery { tenderlyService.hasCapacity() } returns true

        assertThat(service.isEnabled).isTrue()
        val result = service.simulate(order)
        assertThat(result).isEqualTo(OrderSimulation.SUCCESS)
    }

    @Test
    fun `simulation - fail`() = runBlocking<Unit> {
        val order = randomOrder()
        val buyTx: BuyTx = mockk()
        val simulationResult = SimulationResult(status = false)
        coEvery { transactionService.buyTx(any(), any()) } returns buyTx
        coEvery { tenderlyService.simulate(buyTx) } returns simulationResult
        coEvery { tenderlyService.hasCapacity() } returns true

        assertThat(service.isEnabled).isTrue()
        val result = service.simulate(order)
        assertThat(result).isEqualTo(OrderSimulation.FAIL)
    }

    @Test
    fun `simulation - error`() = runBlocking<Unit> {
        val order = randomOrder()
        val buyTx: BuyTx = mockk()
        coEvery { transactionService.buyTx(any(), any()) } returns buyTx
        coEvery { tenderlyService.simulate(buyTx) } throws RuntimeException()
        coEvery { tenderlyService.hasCapacity() } returns true

        assertThat(service.isEnabled).isTrue()
        val result = service.simulate(order)
        assertThat(result).isEqualTo(OrderSimulation.ERROR)
    }
}
