package com.rarible.protocol.order.core.data

import com.rarible.protocol.order.core.model.Order
import org.assertj.core.api.ObjectAssert

fun ObjectAssert<Order?>.isEqualToOrder(expected: Order) {
    this.usingRecursiveComparison().ignoringFields(Order::version.name).isEqualTo(expected)
}
