package com.rarible.protocol.nftorder.listener.evaluator

import com.rarible.protocol.nftorder.core.converter.ShortOrderConverter
import com.rarible.protocol.nftorder.listener.test.data.randomLegacyOrderDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class BestBidOrderComparatorTest {

    @Test
    fun `updated is better - both prices specified`() {
        val current = randomLegacyOrderDto().copy(takePriceUsd = BigDecimal.valueOf(1))
        val updated = randomLegacyOrderDto().copy(takePriceUsd = BigDecimal.valueOf(2))
        val shortCurrent = ShortOrderConverter.convert(current)
        val shortUpdated = ShortOrderConverter.convert(updated)
        val result = BestBidOrderComparator.compare(shortCurrent, shortUpdated)
        assertThat(result).isEqualTo(shortUpdated)
    }

    @Test
    fun `current is better - both prices specified`() {
        val current = randomLegacyOrderDto().copy(takePriceUsd = BigDecimal.valueOf(2))
        val updated = randomLegacyOrderDto().copy(takePriceUsd = BigDecimal.valueOf(1))
        val shortCurrent = ShortOrderConverter.convert(current)
        val shortUpdated = ShortOrderConverter.convert(updated)
        val result = BestBidOrderComparator.compare(shortCurrent, shortUpdated)
        assertThat(result).isEqualTo(shortCurrent)
    }

    @Test
    fun `updated is better - current price not specified`() {
        val current = randomLegacyOrderDto().copy(takePriceUsd = null)
        val updated = randomLegacyOrderDto().copy(takePriceUsd = BigDecimal.valueOf(1))
        val shortCurrent = ShortOrderConverter.convert(current)
        val shortUpdated = ShortOrderConverter.convert(updated)
        val result = BestBidOrderComparator.compare(shortCurrent, shortUpdated)
        assertThat(result).isEqualTo(shortUpdated)
    }

    @Test
    fun `updated is better - no prices specified`() {
        val current = randomLegacyOrderDto().copy(takePriceUsd = null)
        val updated = randomLegacyOrderDto().copy(takePriceUsd = null)
        val shortCurrent = ShortOrderConverter.convert(current)
        val shortUpdated = ShortOrderConverter.convert(updated)
        val result = BestBidOrderComparator.compare(shortCurrent, shortUpdated)
        assertThat(result).isEqualTo(shortUpdated)
    }

}