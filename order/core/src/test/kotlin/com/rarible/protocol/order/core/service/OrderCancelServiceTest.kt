package com.rarible.protocol.order.core.service

import com.rarible.core.test.data.randomBytes
import com.rarible.protocol.order.core.data.createOrderVersion
import com.rarible.protocol.order.core.integration.AbstractIntegrationTest
import com.rarible.protocol.order.core.integration.IntegrationTest
import com.rarible.protocol.order.core.model.OrderStatus
import io.daonomic.rpc.domain.Binary
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
class OrderCancelServiceTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var orderCancelService: OrderCancelService

    @Test
    fun `cancel order - ok`() = runBlocking<Unit> {
        val version = createOrderVersion().copy(
            signature = Binary(randomBytes())
        )
        val saved = save(version)
        assertThat(saved.status).isNotEqualTo(OrderStatus.CANCELLED)
        assertThat(saved.signature).isNotNull

        val result = orderCancelService.cancelOrder(version.hash)

        val canceled = orderRepository.findById(version.hash)
        assertThat(canceled?.status).isEqualTo(OrderStatus.CANCELLED)
        assertThat(canceled?.signature).isNull()
        assertThat(result).isEqualTo(canceled)
    }
}
