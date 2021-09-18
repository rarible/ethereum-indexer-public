package com.rarible.protocol.order.core.misc

import com.rarible.protocol.dto.Continuation
import com.rarible.protocol.dto.OrderFilterDto
import com.rarible.protocol.order.core.model.Order
import java.math.BigDecimal

fun OrderFilterDto.toContinuation(order: Order): String {
    return when (sort) {
        OrderFilterDto.Sort.LAST_UPDATE -> {
            Continuation.LastDate(order.lastUpdateAt, order.hash)
        }
        OrderFilterDto.Sort.TAKE_PRICE_DESC -> {
            Continuation.Price(order.takePriceUsd ?: BigDecimal.ZERO, order.hash)
        }
        OrderFilterDto.Sort.MAKE_PRICE_ASC -> {
            Continuation.Price(order.makePriceUsd ?: BigDecimal.ZERO, order.hash)
        }
    }.toString()
}
