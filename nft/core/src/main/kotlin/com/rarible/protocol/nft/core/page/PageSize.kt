package com.rarible.protocol.nft.core.page

import kotlin.math.min

data class PageSize(
    val default: Int,
    val max: Int
) {
    companion object {
        val TOKEN = PageSize(50, 1000)
        val ITEM = PageSize(50, 1000)
        val ITEM_ACTIVITY = PageSize(50, 1000)
        val OWNERSHIP = PageSize(50, 1000)
    }

    fun limit(size: Int?): Int {
        return min(size ?: default, max)
    }

    fun limit(size: Long?): Long {
        return min(size ?: default.toLong(), max.toLong())
    }
}
