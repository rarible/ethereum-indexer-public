package com.rarible.protocol.nftorder.listener.evaluator

import com.rarible.protocol.nftorder.listener.test.data.randomLegacyOrderDto
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class BestSellOrderComparatorTest {

    @Test
    fun `updated is better - both prices specified`() {
        val current = randomLegacyOrderDto().copy(takePriceUsd = BigDecimal.valueOf(2))
        val updated = randomLegacyOrderDto().copy(takePriceUsd = BigDecimal.valueOf(1))
        val result = BestSellOrderComparator.compare(current, updated)
        Assertions.assertThat(result).isEqualTo(updated)
    }

    @Test
    fun `current is better - both prices specified`() {
        val current = randomLegacyOrderDto().copy(takePriceUsd = BigDecimal.valueOf(1))
        val updated = randomLegacyOrderDto().copy(takePriceUsd = BigDecimal.valueOf(2))
        val result = BestSellOrderComparator.compare(current, updated)
        Assertions.assertThat(result).isEqualTo(current)
    }

    @Test
    fun `updated is better - current price not specified`() {
        val current = randomLegacyOrderDto().copy(takePriceUsd = null)
        val updated = randomLegacyOrderDto().copy(takePriceUsd = BigDecimal.valueOf(1))
        val result = BestSellOrderComparator.compare(current, updated)
        Assertions.assertThat(result).isEqualTo(updated)
    }

    @Test
    fun `updated is better - no prices specified`() {
        val current = randomLegacyOrderDto().copy(takePriceUsd = null)
        val updated = randomLegacyOrderDto().copy(takePriceUsd = null)
        val result = BestSellOrderComparator.compare(current, updated)
        Assertions.assertThat(result).isEqualTo(updated)
    }
}