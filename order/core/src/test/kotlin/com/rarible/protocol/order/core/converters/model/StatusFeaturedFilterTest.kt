package com.rarible.protocol.order.core.converters.model

import com.rarible.protocol.dto.OrderStatusDto
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class StatusFeaturedFilterTest {

    @Test
    fun `all statuses, hide is disabled`() {
        val filter = StatusFeaturedFilter(
            OrderIndexerProperties.FeatureFlags(
                hideInactiveOrders = false
            )
        )

        val nullStatuses = filter.filter(null)
        assertThat(nullStatuses).isNull()

        val allStatuses = filter.filter(OrderStatusDto.values().toList())
        assertThat(allStatuses).containsAll(
            listOf(
                OrderStatusDto.INACTIVE,
                OrderStatusDto.ACTIVE,
                OrderStatusDto.CANCELLED,
                OrderStatusDto.FILLED,
                OrderStatusDto.HISTORICAL
            )
        )
    }

    @Test
    fun `show all, hide is enabled`() {
        val filter = StatusFeaturedFilter(
            OrderIndexerProperties.FeatureFlags(
                hideInactiveOrders = true
            )
        )

        val nullStatuses = filter.filter(null)
        assertThat(nullStatuses).containsAll(
            listOf(
                OrderStatusDto.ACTIVE,
                OrderStatusDto.CANCELLED,
                OrderStatusDto.FILLED,
                OrderStatusDto.HISTORICAL
            )
        )

        val allStatuses = filter.filter(OrderStatusDto.values().toList())
        assertThat(allStatuses).containsAll(
            listOf(
                OrderStatusDto.ACTIVE,
                OrderStatusDto.CANCELLED,
                OrderStatusDto.FILLED,
                OrderStatusDto.HISTORICAL
            )
        )
    }
}
