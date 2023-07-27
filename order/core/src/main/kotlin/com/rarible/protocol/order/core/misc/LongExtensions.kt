package com.rarible.protocol.order.core.misc

fun Long?.orZero(): Long {
    return this ?: 0
}
