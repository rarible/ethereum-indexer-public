package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemMeta
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.time.withTimeout
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * Base API to fetch metadata of items — [ItemMeta].
 */
@Component
class ItemMetaService(
    private val itemMetaResolver: ItemMetaResolver
) {
    suspend fun getAvailableMeta(
        itemId: ItemId,
        demander: String
    ): ItemMeta? {
        logMetaLoading(itemId, "Loading meta synchronously by '$demander'")

        val itemMeta = try {
            itemMetaResolver.resolveItemMeta(itemId)
        } catch (e: Exception) {
            logMetaLoading(itemId, "Synchronous meta loading for '$demander' failed. ${e.message}", warn = true)
            null
        }

        if (itemMeta == null) {
            logMetaLoading(itemId, "Synchronous meta loading for '$demander' failed. Item meta is not found", warn = true)
        }

        return itemMeta
    }

    suspend fun getAvailableMetaWithTimeout(
        itemId: ItemId,
        timeout: Duration,
        demander: String
    ): ItemMeta? {
        return try {
            withTimeout(timeout) {
                getAvailableMeta(
                    itemId = itemId,
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
     * Resolves meta for a pending item and saves it to the cache.
     * It is needed to guarantee that the first sent ItemUpdateEvent goes with an existing meta.
     */
    suspend fun loadAndSavePendingItemMeta(itemId: ItemId, tokenUri: String) {
        logMetaLoading(itemId, "resolving meta for a pending item by $tokenUri")
        val itemMeta = itemMetaResolver.resolvePendingItemMeta(itemId, tokenUri) ?: return
        // TODO in PT-566
        logMetaLoading(itemId, "resolved and saved meta for a pending item by $tokenUri: $itemMeta")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ItemMetaService::class.java)
    }
}
