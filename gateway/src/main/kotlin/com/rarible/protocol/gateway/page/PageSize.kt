package com.rarible.protocol.gateway.page

import kotlin.math.min

data class PageSize(
    val default: Int,
    val max: Int
) {
    companion object {
        val ACTIVITY = PageSize(50, 1000)
    }

    fun limit(size: Int?): Int {
        return min(size ?: default, max)
    }
}
