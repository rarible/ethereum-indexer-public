package com.rarible.protocol.order.core.service.floor

import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.order.OrderFilterBestSellByCollectionByCurrency
import com.rarible.protocol.order.core.model.order.OrderFilterSort
import com.rarible.protocol.order.core.repository.order.OrderRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class FloorSellOrderProvider(
    private val orderRepository: OrderRepository
) {
    suspend fun getCurrencyFloorSells(token: Address) = coroutineScope {
        orderRepository
            .findActiveSellCurrenciesByCollection(token)
            .let { currencies -> currencies + Address.ZERO() } // Eth assert has no toke, so add it as Zero address
            .map { currency -> async { getFloorSellByCurrency(token, currency) } }
            .awaitAll()
            .filterNotNull()
    }

    private suspend fun getFloorSellByCurrency(
        token: Address,
        currency: Address
    ): Order? {
        val filter = OrderFilterBestSellByCollectionByCurrency(
            collection = token,
            sort = OrderFilterSort.MAKE_PRICE_ASC,
            currency = currency,
        )
        return orderRepository.search(filter.toQuery(null, 1)).firstOrNull()
    }
}
