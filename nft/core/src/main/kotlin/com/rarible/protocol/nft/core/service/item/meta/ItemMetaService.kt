package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemMeta
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.time.withTimeout
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * Base API to fetch metadata of items — [ItemMeta].
 */
@Component
class ItemMetaService(
    private val itemMetaResolver: ItemMetaResolver,
    private val pendingItemTokenUriResolver: PendingItemTokenUriResolver
) {
    suspend fun getMeta(
        itemId: ItemId,
        demander: String
    ): ItemMeta? {
        logMetaLoading(itemId, "Loading meta synchronously by '$demander'")

        val itemMeta = try {
            itemMetaResolver.resolveItemMeta(itemId)
        } catch (e: Exception) {
            logMetaLoading(itemId, "Synchronous meta loading for '$demander' failed. ${e.message}", warn = true)
            throw e
        }

        if (itemMeta == null) {
            logMetaLoading(itemId, "Synchronous meta loading for '$demander' failed. Item meta is not found", warn = true)
        }

        return itemMeta
    }

    suspend fun getMetaWithTimeout(
        itemId: ItemId,
        timeout: Duration,
        demander: String
    ): ItemMeta? {
        return try {
            withTimeout(timeout) {
                getMeta(
                    itemId = itemId,
                    demander = demander
                )
            }
        } catch (e: CancellationException) {
            val message = "Timeout synchronously load meta for '$demander' with timeout ${timeout.toMillis()} ms. ${e.message}"
            logMetaLoading(itemId, message, warn = true)
            throw MetaException(message, status = MetaException.Status.Timeout)
        } catch (e: MetaException) {
            val message = "Cannot synchronously load meta for '$demander' with timeout ${timeout.toMillis()} ms. ${e.message}"
            logMetaLoading(itemId, message, warn = true)
            throw e
        } catch (e: Exception) {
            val message = "Cannot synchronously load meta for '$demander' with timeout ${timeout.toMillis()} ms. ${e.message}"
            logMetaLoading(itemId, message, warn = true)
            throw MetaException(message, status = MetaException.Status.Unknown)
        }
    }

    /**
     * Save tokenUri to the cache for a pending utem.
     * It is needed to guarantee that the first sent ItemUpdateEvent goes with an existing meta.
     */
    suspend fun saveTokenUriForPendingItem(itemId: ItemId, tokenUri: String) {
        pendingItemTokenUriResolver.save(itemId, tokenUri)
        logMetaLoading(itemId, "saved tokenUri for a pending item $tokenUri")
    }
}
