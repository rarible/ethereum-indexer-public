package com.rarible.protocol.order.api.controller

import com.rarible.protocol.order.api.integration.AbstractIntegrationTest
import com.rarible.protocol.order.api.integration.IntegrationTest
import com.rarible.protocol.order.core.model.OrderType
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@IntegrationTest
class OrderSettingsControllerFt : AbstractIntegrationTest() {

    @Test
    fun `get fees - ok`() = runBlocking<Unit> {
        val result = orderSettingsClient.getFees().awaitSingle().fees

        assertThat(result.size).isEqualTo(OrderType.values().size)
        assertThat(result["RARIBLE_V1"]).isEqualTo(1)
        assertThat(result["RARIBLE_V2"]).isEqualTo(0)
    }
}
