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

fun trimToLength(str: String, maxLength: Int, suffix: String? = null): String {
    if (str.length < maxLength) {
        return str
    }
    val safeSuffix = suffix ?: ""
    val trimmed = StringBuilder(maxLength + safeSuffix.length)
        .append(str.substring(0, maxLength))
        .append(suffix)

    return trimmed.toString()

}
