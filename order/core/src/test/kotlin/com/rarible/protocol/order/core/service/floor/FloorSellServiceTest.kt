package com.rarible.protocol.order.core.service.floor

import com.rarible.core.test.data.randomAddress
import com.rarible.protocol.order.core.data.randomErc20
import com.rarible.protocol.order.core.data.randomErc721
import com.rarible.protocol.order.core.data.randomOrder
import com.rarible.protocol.order.core.service.CurrencyService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class FloorSellServiceTest {
    private val floorSellOrderProvider = mockk<FloorSellOrderProvider>()
    private val currencyService = mockk<CurrencyService>()
    private val floorSellService = FloorSellService(floorSellOrderProvider, currencyService)

    @Test
    fun `should get floor usd price`() = runBlocking<Unit> {
        val token = randomAddress()
        val currency1 = randomAddress()
        val currency2 = randomAddress()
        val currency3 = randomAddress()

        val order1 = randomOrder().copy(
            make = randomErc721(),
            take = randomErc20(currency1),
            makePrice = BigDecimal.valueOf(1)
        )
        val order2 = randomOrder().copy(
            make = randomErc721(),
            take = randomErc20(currency2),
            makePrice = BigDecimal.valueOf(2)
        )
        val order3 = randomOrder().copy(
            make = randomErc721(),
            take = randomErc20(currency3),
            makePrice = BigDecimal.valueOf(3)
        )
        coEvery { floorSellOrderProvider.getCurrencyFloorSells(token) } returns listOf(order2, order3, order1)
        coEvery { currencyService.getUsdRate(currency1) } returns BigDecimal("9")
        coEvery { currencyService.getUsdRate(currency2) } returns BigDecimal("4")
        coEvery { currencyService.getUsdRate(currency3) } returns BigDecimal("1")
        val floorPrice = floorSellService.getFloorSellPriceUsd(token)
        assertThat(floorPrice).isEqualTo(BigDecimal("3"))
    }

    @Test
    fun `should return null if no floor price`() = runBlocking<Unit> {
        val token = randomAddress()
        coEvery { floorSellOrderProvider.getCurrencyFloorSells(token) } returns emptyList()
        val floorPrice = floorSellService.getFloorSellPriceUsd(token)
        assertThat(floorPrice).isNull()
    }
}
