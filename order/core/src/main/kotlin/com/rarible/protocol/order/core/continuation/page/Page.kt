package com.rarible.protocol.order.core.continuation.page

data class Page<T>(
    @Deprecated("Will be removed into BRAVO-2055")
    val total: Int,
    val continuation: String?,
    val entities: List<T>
) {
    companion object {
        fun <T> empty() = Page<T>(0, null, emptyList<T>())
    }
}
