package com.rarible.protocol.order.core.misc

import java.time.Instant

fun Long?.orZero(): Long {
    return this ?: 0
}