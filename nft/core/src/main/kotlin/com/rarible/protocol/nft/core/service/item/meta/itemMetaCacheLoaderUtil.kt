package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.core.loader.LoadTaskStatus
import com.rarible.loader.cache.CacheEntry
import com.rarible.protocol.nft.core.model.ItemMeta

/**
 * Returns true if loading of the meta for an item has been scheduled in the past,
 * no matter what the loading result is (in progress, failed or success).
 */
fun CacheEntry<ItemMeta>.isMetaInitiallyScheduledForLoading(): Boolean =
    when (this) {
        // Let's use full 'when' expression to not forget some if branches.
        is CacheEntry.NotAvailable -> false
        is CacheEntry.Loaded,
        is CacheEntry.LoadedAndUpdateScheduled,
        is CacheEntry.LoadedAndUpdateFailed,
        is CacheEntry.InitialLoadScheduled,
        is CacheEntry.InitialFailed -> true
    }


/**
 * Returns `true` if the meta for item has been loaded or loading has failed,
 * and `false` if we haven't requested the meta loading or haven't received any result yet.
 */
fun CacheEntry<ItemMeta>.isMetaInitiallyLoadedOrFailed(): Boolean =
    when (this) {
        // Let's use full 'when' expression to not forget some if branches.
        is CacheEntry.Loaded -> true
        is CacheEntry.LoadedAndUpdateScheduled -> true
        is CacheEntry.LoadedAndUpdateFailed -> true
        is CacheEntry.InitialLoadScheduled -> when (loadStatus) {
            is LoadTaskStatus.Scheduled -> false
            is LoadTaskStatus.WaitsForRetry -> true
        }
        is CacheEntry.InitialFailed -> true
        is CacheEntry.NotAvailable -> false
    }

/**
 * Returns meta from this cache entry.
 * If the meta update has failed, the available meta is returned.
 */
fun CacheEntry<ItemMeta>.getAvailable(): ItemMeta? =
    when (this) {
        // Let's use full 'when' expression to not forget some if branches.
        is CacheEntry.Loaded -> data
        is CacheEntry.LoadedAndUpdateScheduled -> data
        is CacheEntry.LoadedAndUpdateFailed -> data
        is CacheEntry.NotAvailable -> null
        is CacheEntry.InitialLoadScheduled -> null
        is CacheEntry.InitialFailed -> null
    }
