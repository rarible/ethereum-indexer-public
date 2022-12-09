package com.rarible.protocol.order.core.service.block.handler

import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

abstract class AbstractEthereumEventHandler<E>(
    private val properties: OrderIndexerProperties.EthereumEventHandleProperties,
    private val handle: suspend (E) -> Unit
) {
    suspend fun handle(events: List<E>) {
        if (properties.parallel) parallel(events) else sequential(events)
    }

    private suspend fun sequential(events: List<E>) {
        for (event in events) {
            handle(event)
        }
    }

    private suspend fun parallel(events: List<E>) = coroutineScope {
        events
            .chunked(properties.chunkSize)
            .map { chunk ->
                chunk
                    .map { event ->
                        async { handle(event) }
                    }
                    .awaitAll()
            }
            .flatten()
            .lastOrNull()
    }
}