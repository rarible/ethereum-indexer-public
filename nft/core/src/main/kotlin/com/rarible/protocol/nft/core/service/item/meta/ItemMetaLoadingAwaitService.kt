package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.loader.cache.CacheEntry
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemMeta
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.time.withTimeout
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.Closeable
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

@Component
class ItemMetaLoadingAwaitService {
    private val awaitingHandles = ConcurrentHashMap<ItemId, AwaitingHandle>()

    private val logger = LoggerFactory.getLogger(ItemMetaLoadingAwaitService::class.java)

    fun onMetaEvent(itemId: ItemId, cacheEntry: CacheEntry<ItemMeta>) {
        awaitingHandles[itemId]?.cacheEntryRef?.set(cacheEntry)
    }

    suspend fun waitForMetaLoadingWithTimeout(
        itemId: ItemId,
        timeout: Duration
    ): ItemMeta? {
        // TODO: if we have 2 instances of the listener, 1 will miss the event and return null. Think how to synchronize them.
        logger.info("Starting to wait for the meta loading of ${itemId.stringValue} for ${timeout.toMillis()} ms")
        return register(itemId).use { awaitingHandle ->
            try {
                withTimeout(timeout) {
                    while (isActive) {
                        val cacheEntry = awaitingHandle.cacheEntryRef.get()
                        if (cacheEntry != null && cacheEntry.isMetaInitiallyLoadedOrFailed()) {
                            return@withTimeout cacheEntry.getAvailable()
                        }
                        delay(100)
                    }
                    return@withTimeout null
                }
            } catch (e: CancellationException) {
                return null
            }
        }
    }

    private fun register(itemId: ItemId): AwaitingHandle {
        val awaitingHandle = AwaitingHandle(itemId)
        awaitingHandles[itemId] = awaitingHandle
        return awaitingHandle
    }

    private fun deregister(itemId: ItemId) {
        awaitingHandles.remove(itemId)
    }

    private inner class AwaitingHandle(val itemId: ItemId) : Closeable {

        val cacheEntryRef = AtomicReference<CacheEntry<ItemMeta>>()

        override fun close() {
            deregister(itemId)
        }
    }
}
