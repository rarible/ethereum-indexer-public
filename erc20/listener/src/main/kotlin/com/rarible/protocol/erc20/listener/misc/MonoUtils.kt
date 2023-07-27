package com.rarible.protocol.erc20.listener.misc

import kotlinx.coroutines.reactive.awaitFirstOrNull
import reactor.core.publisher.Mono

suspend fun <T> Mono<T>.tryAwait(): T? {
    return try {
        awaitFirstOrNull()
    } catch (ex: Exception) {
        null
    }
}
