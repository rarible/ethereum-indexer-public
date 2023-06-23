package com.rarible.protocol.order.core.service.updater

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.data.randomOrder
import com.rarible.protocol.order.core.model.OrderState
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.service.OrderStateService
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CancelInactiveOrderUpdaterTest {

    private val orderStateService: OrderStateService = mockk()
    private val updater = CancelInactiveOrderUpdater(orderStateService)

    @BeforeEach
    fun beforeEach() {
        clearMocks(orderStateService)
    }

    @Test
    fun `inactive order cancelled`() = runBlocking<Unit> {
        val order = randomOrder().copy(platform = Platform.X2Y2, makeStock = EthUInt256.ZERO)

        coEvery { orderStateService.setCancelState(order.hash) } returns OrderState(order.hash, true)

        val updated = updater.update(order)

        assertThat(updated.cancelled).isTrue
        assertThat(updated.status).isEqualTo(OrderStatus.CANCELLED)
    }

    @Test
    fun `non-target orders not affected`() = runBlocking<Unit> {
        // Not x2y2 order, skipped
        val raribleOrder = randomOrder().copy(platform = Platform.RARIBLE, makeStock = EthUInt256.ZERO)
        val updatedRaribleOrder = updater.update(raribleOrder)
        assertThat(updatedRaribleOrder).isEqualTo(raribleOrder)

        // Active order, skipped
        val activeOrder = randomOrder().copy(platform = Platform.X2Y2)
        val updatedActiveOrder = updater.update(activeOrder)
        assertThat(activeOrder).isEqualTo(updatedActiveOrder)
    }
}
