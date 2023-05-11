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
    INACTIVE,

    /**
     * Orders from order_version table
     */
    HISTORICAL;

    companion object {
        val ALL_EXCEPT_HISTORICAL: Set<OrderStatus> = values().toMutableSet() - HISTORICAL
        val ALL_EXCEPT_CANCELLED: Set<OrderStatus> = values().toMutableSet() - CANCELLED - FILLED
    }
}
