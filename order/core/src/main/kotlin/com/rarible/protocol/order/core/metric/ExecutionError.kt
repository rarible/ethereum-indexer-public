package com.rarible.protocol.order.core.metric

enum class ExecutionError(val value: String) {
    SIGNATURE("signature"),
    NO_SIGNATURE("no_signature"),
    API("api")
}
