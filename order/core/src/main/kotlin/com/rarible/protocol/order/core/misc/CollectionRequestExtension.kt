package com.rarible.protocol.order.core.misc

const val DEFAULT_SIZE = 50
const val MAX_SIZE = 1000

fun Int?.limit() = Integer.min(this ?: DEFAULT_SIZE, MAX_SIZE)
