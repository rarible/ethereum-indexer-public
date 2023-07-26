package com.rarible.protocol.order.core.converters.model

import com.rarible.protocol.dto.OrderStatusDto
import com.rarible.protocol.order.core.model.OrderStatus

object OrderStatusConverter {

    fun convert(statuses: List<OrderStatusDto>): List<OrderStatus> {
        val result = statuses.map { OrderStatus.valueOf(it.name) }.toMutableList()
        if (OrderStatus.INACTIVE in result) {
            result += listOf(OrderStatus.ENDED, OrderStatus.NOT_STARTED)
        }
        return result
    }
}
