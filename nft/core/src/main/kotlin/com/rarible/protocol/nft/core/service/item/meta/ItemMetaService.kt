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
    private val itemMetaResolver: ItemMetaResolver
) {

    suspend fun getMeta(
        itemId: ItemId
    ): ItemMeta? {
        logMetaLoading(itemId, "Loading meta synchronously for Item")

        val itemMeta = try {
            itemMetaResolver.resolveItemMeta(itemId)
        } catch (e: Exception) {
            logMetaLoading(itemId, "Synchronous meta loading for Item failed. ${e.message}", warn = true)
            throw e
        }

        if (itemMeta == null) {
            logMetaLoading(itemId, "Synchronous meta loading for Item failed. Item meta is not found", warn = true)
        }

        return itemMeta
    }

    suspend fun getMetaWithTimeout(
        itemId: ItemId,
        timeout: Duration
    ): ItemMeta? {
        return try {
            withTimeout(timeout) {
                getMeta(itemId)
            }
        } catch (e: CancellationException) {
            val message = "Item meta load timeout (${timeout.toMillis()}ms) - ${e.message}"
            logMetaLoading(itemId, message, warn = true)
            throw MetaException(message, status = MetaException.Status.Timeout)
        } catch (e: MetaException) {
            val message = "Item meta load failed (${e.status}) - ${e.message}"
            logMetaLoading(itemId, message, warn = true)
            throw e
        } catch (e: Exception) {
            val message = "Item meta load failed with unexpected error - ${e.message}"
            logMetaLoading(itemId, message, warn = true)
            throw MetaException(message, status = MetaException.Status.Unknown)
        }
    }
}
