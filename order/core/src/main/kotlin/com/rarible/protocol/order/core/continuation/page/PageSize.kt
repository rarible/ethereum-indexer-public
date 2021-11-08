package com.rarible.protocol.order.core.continuation.page

data class PageSize(
    val default: Int,
    val max: Int
) {
    companion object {
        val AUCTION = PageSize(50, 1000)
    }

    fun limit(size: Int?): Int {
        return Integer.min(size ?: default, max)
    }
}
