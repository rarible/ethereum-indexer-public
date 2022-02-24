package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.loader.cache.CacheLoaderService
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemMeta
import kotlinx.coroutines.time.withTimeout
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * Base API to fetch metadata of items — [ItemMeta].
 */
@Component
class ItemMetaService(
    @Qualifier("meta.cache.loader.service")
    private val itemMetaCacheLoaderService: CacheLoaderService<ItemMeta>,
    private val itemMetaCacheLoader: ItemMetaCacheLoader
) {

    private val logger = LoggerFactory.getLogger(ItemMetaService::class.java)

    /**
     * Return available meta or `null` if it hasn't been loaded,
     * has failed, or hasn't been requested yet.
     * Schedule an update in the last case.
     */
    suspend fun getAvailableMetaOrScheduleLoading(itemId: ItemId): ItemMeta? =
        getAvailableMetaOrLoadSynchronously(itemId = itemId, synchronous = false)

    /**
     * Return available meta, if any. Otherwise, load the meta in the current coroutine (it may be slow).
     * Additionally, schedule loading if the meta hasn't been requested for this item.
     */
    suspend fun getAvailableMetaOrLoadSynchronously(
        itemId: ItemId,
        synchronous: Boolean
    ): ItemMeta? {
        val metaCacheEntry = itemMetaCacheLoaderService.get(itemId.toCacheKey())
        val availableMeta = metaCacheEntry.getAvailable()
        if (availableMeta != null) {
            return availableMeta
        }
        if (metaCacheEntry.isMetaInitiallyLoadedOrFailed()) {
            return null
        }
        if (!metaCacheEntry.isMetaInitiallyScheduledForLoading()) {
            scheduleLoading(itemId)
        }
        if (synchronous) {
            return itemMetaCacheLoader.load(itemId.toCacheKey())
        }
        return null
    }

    suspend fun getAvailableMetaOrLoadSynchronouslyWithTimeout(
        itemId: ItemId,
        timeout: Duration
    ): ItemMeta? {
        return try {
            withTimeout(timeout) {
                getAvailableMetaOrLoadSynchronously(
                    itemId = itemId,
                    synchronous = true
                )
            }
        } catch (e: Exception) {
            logger.error("Cannot synchronously load meta for $itemId with timeout ${timeout.toMillis()} ms", e)
            null
        }
    }

    /**
     * Schedule an update (or initial loading) of metadata.
     */
    suspend fun scheduleLoading(itemId: ItemId) {
        logger.info("Scheduling meta update for {}", itemId.toCacheKey())
        itemMetaCacheLoaderService.update(itemId.toCacheKey())
    }

    /**
     * Schedule an update (or initial loading) of metadata.
     */
    suspend fun scheduleMetaUpdate(itemId: ItemId) {
        logMetaLoading(itemId, "scheduling update")
        itemMetaCacheLoaderService.update(itemId.toCacheKey())
    }

    /**
     * Remove metadata for an item. After this method returns, [getAvailable] will return `null`
     * unless another scheduled update has been executed.
     */
    suspend fun removeMeta(itemId: ItemId) {
        logMetaLoading(itemId, "removing meta")
        itemMetaCacheLoaderService.remove(itemId.toCacheKey())
    }

    private fun ItemId.toCacheKey(): String = decimalStringValue
}
