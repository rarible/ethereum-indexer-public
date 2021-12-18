package com.rarible.protocol.order.core.misc

import com.rarible.protocol.dto.Continuation
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.order.Filter
import java.math.BigDecimal

fun Filter.toContinuation(order: Order): String {
    return when (sort) {
        Filter.Sort.LAST_UPDATE_DESC -> {
            Continuation.LastDate(order.lastUpdateAt, order.hash)
        }
        Filter.Sort.LAST_UPDATE_ASC -> {
            Continuation.LastDate(order.lastUpdateAt, order.hash)
        }
        Filter.Sort.TAKE_PRICE_DESC -> {
            Continuation.Price(order.takePriceUsd ?: BigDecimal.ZERO, order.hash)
        }
        Filter.Sort.MAKE_PRICE_ASC -> {
            Continuation.Price(order.makePriceUsd ?: BigDecimal.ZERO, order.hash)
        }
    }.toString()
}
