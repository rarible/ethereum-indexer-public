package com.rarible.protocol.order.core.service.block.handler

import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

abstract class AbstractEthereumEventHandler<E, M>(
    private val properties: OrderIndexerProperties.EthereumEventHandleProperties,
) {
    suspend fun handle(events: List<E>) {
        val mapped = map(events)
        if (properties.parallel) parallel(mapped) else sequential(mapped)
    }

    protected abstract suspend fun handleSingle(event: M)

    protected abstract fun map(events: List<E>): List<M>

    private suspend fun sequential(events: List<M>) {
        for (event in events) {
            handleSingle(event)
        }
    }

    private suspend fun parallel(events: List<M>) = coroutineScope {
        events
            .chunked(properties.chunkSize)
            .map { chunk ->
                chunk
                    .map { event ->
                        async { handleSingle(event) }
                    }
                    .awaitAll()
            }
            .flatten()
            .lastOrNull()
    }
}
