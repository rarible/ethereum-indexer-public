package com.rarible.protocol.order.core.continuation

interface ContinuationFactory<T, out C : Continuation> {
    fun getContinuation(entity: T): C
}
