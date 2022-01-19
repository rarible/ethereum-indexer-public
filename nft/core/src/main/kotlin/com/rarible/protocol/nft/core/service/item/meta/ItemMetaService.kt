package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.core.loader.LoadTaskStatus
import com.rarible.loader.cache.CacheEntry
import com.rarible.loader.cache.CacheLoaderService
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemMeta
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.time.withTimeoutOrNull
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * base api to fetch metadata of items — [ItemMeta].
 *
 * Loading of metadata is performed in background by 'cache-loader' library ([scheduleMetaUpdate]).
 * [ItemMetaCacheLoader] delegates to [ItemMetaResolver] to resolve actual metadata.
 * Loaded metadata are cached in the database and can be requested with [getAvailable] or [getAvailableMetaOrScheduleLoading].
 */
@Component
class ItemMetaService(
    @Qualifier("meta.cache.loader.service")
    private val itemMetaCacheLoaderService: CacheLoaderService<ItemMeta>
) {

    /**
     * Get available item metadata. Return `null` if the meta hasn't been requested yet,
     * has failed to be loaded or is being loaded now.
     */
    suspend fun getAvailable(itemId: ItemId): ItemMeta? =
        itemMetaCacheLoaderService.getAvailable(itemId.toCacheKey())

    /**
     * Return available meta (same as [getAvailable]) but additionally schedule an update
     * if the meta hasn't been requested yet.
     */
    suspend fun getAvailableMetaOrScheduleLoading(itemId: ItemId): ItemMeta? {
        val availableMeta = getAvailable(itemId)
        if (availableMeta == null) {
            if (!isMetaLoadingInitiallyScheduled(itemId)) {
                scheduleMetaUpdate(itemId)
            }
        }
        return availableMeta
    }

    /**
     * Return available meta (same as [getAvailable]) if the meta has been loaded
     * or its loading has failed (`null` in this case), or schedule a meta update
     * and wait up to [timeout] until the meta is loaded or failed.
     */
    suspend fun getAvailableMetaOrScheduleAndWait(
        itemId: ItemId,
        timeout: Duration
    ): ItemMeta? {
        val availableMeta = getAvailableMetaOrScheduleLoading(itemId)
        if (availableMeta != null) {
            return availableMeta
        }
        logMetaLoading(itemId, "Starting to wait for initial loading for ${timeout.toMillis()} millis")
        return withTimeoutOrNull(timeout) {
            while (isActive) {
                if (isMetaInitiallyLoadedOrFailed(itemId)) {
                    return@withTimeoutOrNull getAvailable(itemId)
                }
                delay(100)
            }
            return@withTimeoutOrNull null
        }
    }

    /**
     * Returns true if loading of the meta for an item has been scheduled in the past,
     * no matter what the loading result is (in progress, failed or success).
     */
    suspend fun isMetaLoadingInitiallyScheduled(itemId: ItemId): Boolean =
        when (val cacheEntry = itemMetaCacheLoaderService.get(itemId.toCacheKey())) {
            is CacheEntry.Loaded -> true
            is CacheEntry.LoadedAndUpdateScheduled -> true
            is CacheEntry.LoadedAndUpdateFailed -> true
            is CacheEntry.InitialLoadScheduled -> when (cacheEntry.loadStatus) {
                is LoadTaskStatus.Scheduled -> true
                is LoadTaskStatus.WaitsForRetry -> true
            }
            is CacheEntry.InitialFailed -> true
            is CacheEntry.NotAvailable -> false
        }

    /**
     * Returns `true` if the meta for item has been loaded or loading has failed,
     * and `false` if we haven't requested the meta loading or haven't received any result yet.
     */
    suspend fun isMetaInitiallyLoadedOrFailed(itemId: ItemId): Boolean =
        when (val cacheEntry = itemMetaCacheLoaderService.get(itemId.toCacheKey())) {
            is CacheEntry.Loaded -> true
            is CacheEntry.LoadedAndUpdateScheduled -> true
            is CacheEntry.LoadedAndUpdateFailed -> true
            is CacheEntry.InitialLoadScheduled -> when (cacheEntry.loadStatus) {
                is LoadTaskStatus.Scheduled -> false
                is LoadTaskStatus.WaitsForRetry -> true
            }
            is CacheEntry.InitialFailed -> true
            is CacheEntry.NotAvailable -> false
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
