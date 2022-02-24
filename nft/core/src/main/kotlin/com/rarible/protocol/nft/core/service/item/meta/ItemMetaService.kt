package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.loader.cache.CacheLoaderService
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemMeta
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * Base API to fetch metadata of items — [ItemMeta].
 *
 * Loading of metadata is performed in background by 'cache-loader' library ([scheduleMetaUpdate]).
 * [ItemMetaCacheLoader] delegates to [ItemMetaResolver] to resolve actual metadata.
 */
@Component
class ItemMetaService(
    @Qualifier("meta.cache.loader.service")
    private val itemMetaCacheLoaderService: CacheLoaderService<ItemMeta>,
    private val itemMetaLoadingAwaitService: ItemMetaLoadingAwaitService
) {

    private val logger = LoggerFactory.getLogger(ItemMetaService::class.java)

    /**
     * Return available meta or `null` if it hasn't been loaded, has failed, or hasn't been requested yet.
     * Schedule an update in the last case.
     */
    suspend fun getAvailableMetaOrScheduleLoading(itemId: ItemId): ItemMeta? =
        getAvailableMetaOrScheduleLoadingAndWaitWithTimeout(itemId, null)

    /**
     * Same as [getAvailableMetaOrScheduleLoading] and synchronously (in a coroutine) wait up to
     * [timeout] until the meta is loaded or failed.
     */
    suspend fun getAvailableMetaOrScheduleLoadingAndWaitWithTimeout(
        itemId: ItemId,
        timeout: Duration?
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
        if (timeout == null) {
            return null
        }
        return itemMetaLoadingAwaitService.waitForMetaLoadingWithTimeout(itemId, timeout)
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
