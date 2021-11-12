package com.rarible.protocol.order.core.continuation.page

import com.rarible.protocol.order.core.continuation.Continuation
import com.rarible.protocol.order.core.continuation.ContinuationFactory

class Paging<T, C : Continuation, F : ContinuationFactory<T, C>>(
    private val factory: F,
    private val entities: List<T>
) {
    fun getPage(size: Int): Page<T> {
        if (entities.isEmpty()) return Page.empty()
        val continuation = if (entities.size >= size) factory.getContinuation(entities.last()) else null

        return Page(
            total = entities.size,
            continuation = continuation?.toString(),
            entities = entities
        )
    }
}
