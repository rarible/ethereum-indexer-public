package com.rarible.protocol.nftorder.listener.evaluator

import com.rarible.protocol.nftorder.core.converter.ShortOrderConverter
import com.rarible.protocol.nftorder.listener.test.data.randomLegacyOrderDto
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class BestSellOrderComparatorTest {

    @Test
    fun `updated is better - both prices specified`() {
        val current = randomLegacyOrderDto().copy(makePriceUsd = BigDecimal.valueOf(2))
        val updated = randomLegacyOrderDto().copy(makePriceUsd = BigDecimal.valueOf(1))
        val shortCurrent = ShortOrderConverter.convert(current)
        val shortUpdated = ShortOrderConverter.convert(updated)
        val result = BestSellOrderComparator.compare(shortCurrent, shortUpdated)
        Assertions.assertThat(result).isEqualTo(shortUpdated)
    }

    @Test
    fun `current is better - both prices specified`() {
        val current = randomLegacyOrderDto().copy(makePriceUsd = BigDecimal.valueOf(1))
        val updated = randomLegacyOrderDto().copy(makePriceUsd = BigDecimal.valueOf(2))
        val shortCurrent = ShortOrderConverter.convert(current)
        val shortUpdated = ShortOrderConverter.convert(updated)
        val result = BestSellOrderComparator.compare(shortCurrent, shortUpdated)
        Assertions.assertThat(result).isEqualTo(shortCurrent)
    }

    @Test
    fun `updated is better - current price not specified`() {
        val current = randomLegacyOrderDto().copy(makePriceUsd = null)
        val updated = randomLegacyOrderDto().copy(makePriceUsd = BigDecimal.valueOf(1))
        val shortCurrent = ShortOrderConverter.convert(current)
        val shortUpdated = ShortOrderConverter.convert(updated)
        val result = BestSellOrderComparator.compare(shortCurrent, shortUpdated)
        Assertions.assertThat(result).isEqualTo(shortUpdated)
    }

    @Test
    fun `updated is better - no prices specified`() {
        val current = randomLegacyOrderDto().copy(makePriceUsd = null)
        val updated = randomLegacyOrderDto().copy(makePriceUsd = null)
        val shortCurrent = ShortOrderConverter.convert(current)
        val shortUpdated = ShortOrderConverter.convert(updated)
        val result = BestSellOrderComparator.compare(shortCurrent, shortUpdated)
        Assertions.assertThat(result).isEqualTo(shortUpdated)
    }
}