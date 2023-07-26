package com.rarible.protocol.order.core.service

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigDecimal
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class CurrencyServiceTest {
    private val priceUpdateService = mockk<PriceUpdateService>()
    private val currencyService = CurrencyService(priceUpdateService)

    @Test
    fun `should get currency price and then get it from cache`() = runBlocking<Unit> {
        val currency = randomAddress()
        val rate = randomBigDecimal()
        coEvery { priceUpdateService.getTokenRate(currency, any()) } returns rate

        val fetchedRate = currencyService.getUsdRate(currency)
        assertThat(fetchedRate).isEqualTo(rate)

        val changedRate = currencyService.getUsdRate(currency)
        assertThat(changedRate).isEqualTo(rate)

        coVerify(exactly = 1) { priceUpdateService.getTokenRate(currency, any()) }
    }

    @Test
    fun `should should refresh cached rate`() = runBlocking<Unit> {
        val currency = randomAddress()
        val rate = randomBigDecimal()
        val newRate = randomBigDecimal()
        coEvery { priceUpdateService.getTokenRate(currency, any()) } returnsMany listOf(rate, newRate)
        val fetchedRate = currencyService.getUsdRate(currency)
        assertThat(fetchedRate).isEqualTo(rate)

        currencyService.refreshCache()
        val refreshedRate = currencyService.getUsdRate(currency)
        assertThat(refreshedRate).isEqualTo(newRate)
        coVerify(exactly = 2) { priceUpdateService.getTokenRate(currency, any()) }
    }
}
