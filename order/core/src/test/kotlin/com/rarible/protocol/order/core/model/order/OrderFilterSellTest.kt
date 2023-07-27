package com.rarible.protocol.order.core.model.order

import com.rarible.protocol.dto.OrderStatusDto
import com.rarible.protocol.dto.PlatformDto
import com.rarible.protocol.order.core.repository.order.OrderRepositoryIndexes
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class OrderFilterSellTest {
    @Test
    fun `set null hint for empty status and platform`() {
        val orderFilterSell = OrderFilterSell(
            origin = null,
            platforms = emptyList(),
            status = emptyList(),
            sort = OrderFilterSort.LAST_UPDATE_ASC
        )
        assertThat(orderFilterSell.toQuery("test", 100).hint)
            .isNull()
    }

    @Test
    fun `set SELL_ORDERS_PLATFORM_STATUS_DEFINITION hint for single status and platform`() {
        val orderFilterSell = OrderFilterSell(
            origin = null,
            platforms = listOf(PlatformDto.RARIBLE),
            status = listOf(OrderStatusDto.ACTIVE),
            sort = OrderFilterSort.LAST_UPDATE_ASC
        )
        // with continuation
        assertThat(orderFilterSell.toQuery("test", 100).hint)
            .isEqualTo(OrderRepositoryIndexes.SELL_ORDERS_PLATFORM_STATUS_DEFINITION.indexKeys.toJson())

        // without continuation
        assertThat(orderFilterSell.toQuery(null, 100).hint)
            .isEqualTo(OrderRepositoryIndexes.SELL_ORDERS_PLATFORM_STATUS_DEFINITION.indexKeys.toJson())
    }

    @Test
    fun `set SELL_ORDERS_PLATFORM_DEFINITION hint for single platform`() {
        val orderFilterSellNoStatus = OrderFilterSell(
            origin = null,
            platforms = listOf(PlatformDto.RARIBLE),
            status = emptyList(),
            sort = OrderFilterSort.LAST_UPDATE_ASC
        )
        val orderFilterSellManyStatuses = OrderFilterSell(
            origin = null,
            platforms = listOf(PlatformDto.RARIBLE),
            status = listOf(OrderStatusDto.ACTIVE, OrderStatusDto.FILLED),
            sort = OrderFilterSort.LAST_UPDATE_ASC
        )

        // with continuation
        assertThat(orderFilterSellNoStatus.toQuery("test", 100).hint)
            .isEqualTo(OrderRepositoryIndexes.SELL_ORDERS_PLATFORM_DEFINITION.indexKeys.toJson())
        assertThat(orderFilterSellManyStatuses.toQuery("test", 100).hint)
            .isEqualTo(OrderRepositoryIndexes.SELL_ORDERS_PLATFORM_DEFINITION.indexKeys.toJson())
    }

    @Test
    fun `set SELL_ORDERS_STATUS_DEFINITION hint for single platform`() {
        val orderFilterSellNoPlatform = OrderFilterSell(
            origin = null,
            platforms = emptyList(),
            status = listOf(OrderStatusDto.ACTIVE),
            sort = OrderFilterSort.LAST_UPDATE_ASC
        )
        val orderFilterSellManyPlatforms = OrderFilterSell(
            origin = null,
            platforms = listOf(PlatformDto.RARIBLE, PlatformDto.OPEN_SEA),
            status = listOf(OrderStatusDto.ACTIVE),
            sort = OrderFilterSort.LAST_UPDATE_ASC
        )

        assertThat(orderFilterSellNoPlatform.toQuery("test", 100).hint)
            .isEqualTo(OrderRepositoryIndexes.SELL_ORDERS_STATUS_DEFINITION.indexKeys.toJson())
        assertThat(orderFilterSellManyPlatforms.toQuery("test", 100).hint)
            .isEqualTo(OrderRepositoryIndexes.SELL_ORDERS_STATUS_DEFINITION.indexKeys.toJson())
    }

    @Test
    fun `set null hint for many platforms and statuses`() {
        val orderFilterSell = OrderFilterSell(
            origin = null,
            platforms = listOf(PlatformDto.RARIBLE, PlatformDto.OPEN_SEA),
            status = listOf(OrderStatusDto.ACTIVE, OrderStatusDto.FILLED),
            sort = OrderFilterSort.LAST_UPDATE_ASC
        )
        // with continuation
        assertThat(orderFilterSell.toQuery("test", 100).hint)
            .isNull()

        // without continuation
        assertThat(orderFilterSell.toQuery(null, 100).hint)
            .isEqualTo(null)
    }
}
