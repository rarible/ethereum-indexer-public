package com.rarible.protocol.order.core.service.block.filter

import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.data.createLogRecord
import com.rarible.protocol.order.core.data.createOrderSideMatch
import com.rarible.protocol.order.core.model.HistorySource
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class SourceOrderEventHandleFilterTest {
    private val properties = mockk<OrderIndexerProperties.OrderEventHandleProperties>()
    private val filter = SourceOrderEventHandleFilter(properties)

    @Test
    fun `filter - ok for seaport`() {
        every { properties.handleSeaport } returns true
        val event = createLogRecord(createOrderSideMatch().copy(source = HistorySource.OPEN_SEA))
        assertThat(filter.filter(event)).isTrue
    }

    @Test
    fun `filter - false for seaport`() {
        every { properties.handleSeaport } returns false
        val event = createLogRecord(createOrderSideMatch().copy(source = HistorySource.OPEN_SEA))
        assertThat(filter.filter(event)).isFalse
    }

    @Test
    fun `filter - ok for all other besides seaport`() {
        every { properties.handleSeaport } returns false
        HistorySource
            .values()
            .filter { it != HistorySource.OPEN_SEA }
            .forEach { source ->
                val event = createLogRecord(createOrderSideMatch().copy(source = source))
                assertThat(filter.filter(event)).isTrue()
            }
    }
}
