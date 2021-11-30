package com.rarible.protocol.order.core.model

enum class OrderStatus {

    /**
     * makeStock > 0 && order matches start, end interval
     */
    ACTIVE,

    /**
     * canceled == true
     */
    CANCELLED,

    /**
     * fill == take.value
     */
    FILLED,

    /**
     * order where start < now && makeStock > 0
     */
    NOT_STARTED,

    /**
     * order where end > now
     */
    ENDED,

    /**
     * makeStock == 0
     */
    INACTIVE
}
