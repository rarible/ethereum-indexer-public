package com.rarible.protocol.nft.core.service.item.reduce

import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

class ReduceContext(val skipOwnerships: Boolean) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<ReduceContext>
}
