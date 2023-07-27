package com.rarible.protocol.order.listener.service.descriptors.exchange.v1

import com.rarible.protocol.contracts.exchange.v1.BuyEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BuyEventTest {
    @Test
    fun `fill is calculated correctly`() {
        val e = BuyEvent(
            null,
            null,
            null,
            10.toBigInteger(),
            null,
            null,
            null,
            5.toBigInteger(),
            null,
            6.toBigInteger(),
            null)

        assertThat(e.fill).isEqualTo(3.toBigInteger())
    }
}
