package com.rarible.protocol.order.core.continuation.page

import kotlin.math.min

data class PageSize(
    val default: Int,
    val max: Int
) {
    companion object {
        val ORDER = PageSize(50, 1000)
        val ORDER_BID = PageSize(50, 1000)
        val ORDER_ACTIVITY = PageSize(50, 1000)
        val ORDER_AGGREGATION = PageSize(50, 1000)
        val AUCTION = PageSize(50, 1000)
        val AUCTION_BIDS = PageSize(50, 1000)
    }

    fun limit(size: Int?): Int {
        return min(size ?: default, max)
    }

    fun limit(size: Long?): Long {
        return min(size ?: default.toLong(), max.toLong())
    }
}
