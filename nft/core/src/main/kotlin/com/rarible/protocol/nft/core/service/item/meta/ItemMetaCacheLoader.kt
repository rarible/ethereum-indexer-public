package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.core.apm.CaptureSpan
import com.rarible.loader.cache.CacheLoader
import com.rarible.loader.cache.CacheType
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemMeta
import com.rarible.protocol.nft.core.service.item.meta.ItemMetaCacheLoader.Companion.TYPE
import com.rarible.protocol.nft.core.service.item.meta.descriptors.ITEM_META_CAPTURE_SPAN_TYPE
import org.springframework.stereotype.Component

/**
 * [CacheLoader] implementation that resolves and caches items' metadata [ItemMeta].
 * This is not called directly from application code.
 * The [ItemMetaCacheLoader] is registered as a bean of type [TYPE] in the cache loader library
 * and is called internally when [ItemMetaService] resolves meta.
 */
@Component
@CaptureSpan(type = ITEM_META_CAPTURE_SPAN_TYPE)
class ItemMetaCacheLoader(
    private val itemMetaResolver: ItemMetaResolver
) : CacheLoader<ItemMeta> {

    override val type: CacheType get() = TYPE

    override suspend fun load(key: String): ItemMeta {
        val itemId = ItemId.parseId(key)
        return try {
            itemMetaResolver.resolveItemMeta(itemId)
        } catch (e: Exception) {
            throw ItemMetaResolutionException("Failed to resolve meta for $itemId", e)
        } ?: throw ItemMetaResolutionException("Item meta is not found for $itemId")
    }

    /**
     * An exception thrown when item metadata cannot be resolved.
     */
    class ItemMetaResolutionException : RuntimeException {
        constructor(message: String, cause: Throwable) : super(message, cause)
        constructor(message: String) : super(message)
    }

    companion object {
        const val TYPE: CacheType = "item-meta"
    }
}
