package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.loader.cache.CacheLoaderService
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemMeta
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.time.withTimeout
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.dao.DuplicateKeyException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * Base API to fetch metadata of items — [ItemMeta].
 */
@Component
class ItemMetaService(
    @Qualifier("meta.cache.loader.service")
    private val itemMetaCacheLoaderService: CacheLoaderService<ItemMeta>,
    private val itemMetaCacheLoader: ItemMetaCacheLoader,
    private val itemMetaResolver: ItemMetaResolver,
    private val properties: NftIndexerProperties
) {

    private val logger = LoggerFactory.getLogger(ItemMetaService::class.java)

    /**
     * Return available meta or `null` if it hasn't been loaded,
     * has failed, or hasn't been requested yet.
     * Schedule an update in the last case.
     */
    suspend fun getAvailableMetaOrScheduleLoading(
        itemId: ItemId,
        demander: String
    ): ItemMeta? =
        getAvailableMetaOrLoadSynchronously(itemId = itemId, synchronous = false, demander = demander)

    /**
     * Return available meta, if any. Otherwise, load the meta in the current coroutine (it may be slow).
     * Additionally, schedule loading if the meta hasn't been requested for this item.
     */
    suspend fun getAvailableMetaOrLoadSynchronously(
        itemId: ItemId,
        synchronous: Boolean,
        demander: String,
        scheduleIfNeeded: Boolean = true
    ): ItemMeta? {
        if (properties.enableMetaCache) {
            val metaCacheEntry = itemMetaCacheLoaderService.get(itemId.toCacheKey())
            val availableMeta = metaCacheEntry.getAvailable()
            if (availableMeta != null) {
                return availableMeta
            }
            if (metaCacheEntry.isMetaInitiallyLoadedOrFailed()) {
                return null
            }
            if (scheduleIfNeeded && !synchronous && !metaCacheEntry.isMetaInitiallyScheduledForLoading()) {
                scheduleMetaUpdate(itemId, demander)
            }
        }
        if (synchronous) {
            logMetaLoading(itemId, "Loading meta synchronously by '$demander'")
            val itemMeta = try {
                itemMetaCacheLoader.load(itemId.toCacheKey())
            } catch (e: ItemMetaCacheLoader.ItemMetaResolutionException) {
                logMetaLoading(itemId, "Synchronous meta loading for '$demander' failed for $itemId", warn = true)
                null
            }
            if (itemMeta != null) {
                logMetaLoading(itemId, "Saving synchronously loaded meta for '$demander' to the cache")
                try {
                    itemMetaCacheLoaderService.save(itemId.toCacheKey(), itemMeta)
                } catch (e: Exception) {
                    if (e !is OptimisticLockingFailureException && e !is DuplicateKeyException) {
                        logMetaLoading(itemId, "Failed to save synchronously loaded meta to cache", warn = true)
                        throw e
                    }
                }
            }
            return itemMeta
        }
        return null
    }

    suspend fun getAvailableMetaOrLoadSynchronouslyWithTimeout(
        itemId: ItemId,
        timeout: Duration,
        demander: String
    ): ItemMeta? {
        return try {
            withTimeout(timeout) {
                getAvailableMetaOrLoadSynchronously(
                    itemId = itemId,
                    synchronous = true,
                    demander = demander
                )
            }
        } catch (e: CancellationException) {
            logger.warn("Timeout synchronously load meta by $itemId for '$demander' with timeout ${timeout.toMillis()} ms", e)
            null
        } catch (e: Exception) {
            logger.error("Cannot synchronously load meta by $itemId for '$demander' with timeout ${timeout.toMillis()} ms", e)
            null
        }
    }

    /**
     * Schedule an update (or initial loading) of metadata.
     */
    suspend fun scheduleMetaUpdate(itemId: ItemId, demander: String) {
        logMetaLoading(itemId, "scheduling update requested by '$demander'")
        itemMetaCacheLoaderService.update(itemId.toCacheKey())
    }

    /**
     * Remove metadata for an item. After this method returns, [getAvailable] will return `null`
     * unless another scheduled update has been executed.
     */
    suspend fun removeMeta(itemId: ItemId, demander: String) {
        logMetaLoading(itemId, "removing meta requested by '$demander'")
        itemMetaCacheLoaderService.remove(itemId.toCacheKey())
    }

    /**
     * Resolves meta for a pending item and saves it to the cache.
     * It is needed to guarantee that the first sent ItemUpdateEvent goes with an existing meta.
     */
    suspend fun loadAndSavePendingItemMeta(itemId: ItemId, tokenUri: String) {
        logMetaLoading(itemId, "resolving meta for a pending item by $tokenUri")
        val itemMeta = itemMetaResolver.resolvePendingItemMeta(itemId, tokenUri) ?: return
        itemMetaCacheLoaderService.save(itemId.toCacheKey(), itemMeta)
        logMetaLoading(itemId, "resolved and saved meta for a pending item by $tokenUri: $itemMeta")
    }
}

fun ItemId.toCacheKey(): String = decimalStringValue
