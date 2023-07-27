package com.rarible.protocol.order.core.service.floor

import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.token
import com.rarible.protocol.order.core.service.CurrencyService
import org.springframework.stereotype.Component
import scalether.domain.Address
import java.math.BigDecimal

@Component
class FloorSellService(
    private val floorSellOrderProvider: FloorSellOrderProvider,
    private val currencyService: CurrencyService
) {
    suspend fun getFloorSellPriceUsd(token: Address): BigDecimal? {
        return floorSellOrderProvider
            .getCurrencyFloorSells(token)
            .mapNotNull { calculateRate(it) }
            .minOrNull()
    }

    private suspend fun calculateRate(order: Order): BigDecimal? {
        val makePrice = order.makePrice ?: return null
        val currency = order.take.type.token
        val usdRate = currencyService.getUsdRate(currency) ?: return null
        return makePrice * usdRate
    }
}
