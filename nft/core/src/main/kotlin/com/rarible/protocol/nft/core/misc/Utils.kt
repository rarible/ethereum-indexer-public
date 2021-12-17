package com.rarible.protocol.nft.core.misc

import com.rarible.core.entity.reducer.chain.ReducersChain
import com.rarible.core.entity.reducer.service.Reducer

fun <T : Any> List<T>.ifNotEmpty(): List<T>? {
    return if (isNotEmpty()) this else null
}

fun <Event, Entity> combineIntoChain(
    vararg reducers: Reducer<Event, Entity>,
): Reducer<Event, Entity> {
    return ReducersChain(reducers.asList())
}
