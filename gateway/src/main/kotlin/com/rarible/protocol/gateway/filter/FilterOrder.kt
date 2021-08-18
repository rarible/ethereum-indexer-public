package com.rarible.protocol.gateway.filter

import org.springframework.core.Ordered

internal enum class FilterOrder(val order: Int) {
    TRACING(Ordered.HIGHEST_PRECEDENCE)
}