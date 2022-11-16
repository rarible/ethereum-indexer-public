package com.rarible.protocol.order.core.service.updater

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.data.createOrder
import com.rarible.protocol.order.core.model.OrderState
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.repository.order.OrderStateRepository
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class X2Y2OrderUpdaterTest {

    private val orderStateRepository: OrderStateRepository = mockk()
    private val updater = X2Y2OrderUpdater(orderStateRepository)

    @BeforeEach
    fun beforeEach() {
        clearMocks(orderStateRepository)
    }

    @Test
    fun `inactive order cancelled`() = runBlocking<Unit> {
        val order = createOrder().copy(platform = Platform.X2Y2, makeStock = EthUInt256.ZERO)

        coEvery { orderStateRepository.getById(order.hash) } returns null
        coEvery { orderStateRepository.save(any()) } answers { it.invocation.args[0] as OrderState }

        val updated = updater.update(order)

        assertThat(updated.cancelled).isTrue
        assertThat(updated.status).isEqualTo(OrderStatus.CANCELLED)

        coVerify(exactly = 1) {
            orderStateRepository.save(match {
                it.id == order.hash && it.canceled
            })
        }
    }

    @Test
    fun `non-target orders not affected`() = runBlocking<Unit> {
        // Not x2y2 order, skipped
        val raribleOrder = createOrder().copy(platform = Platform.RARIBLE, makeStock = EthUInt256.ZERO)
        val updatedRaribleOrder = updater.update(raribleOrder)
        assertThat(updatedRaribleOrder).isEqualTo(raribleOrder)

        // Active order, skipped
        val activeOrder = createOrder().copy(platform = Platform.X2Y2)
        val updatedActiveOrder = updater.update(activeOrder)
        assertThat(activeOrder).isEqualTo(updatedActiveOrder)
    }
}
