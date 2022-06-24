package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemMeta
import com.rarible.protocol.nft.core.service.item.meta.descriptors.RariblePropertiesResolver
import org.springframework.stereotype.Component

@Component
class ItemMetaResolver(
    private val itemPropertiesService: ItemPropertiesService,
    private val rariblePropertiesResolver: RariblePropertiesResolver
) {

    suspend fun resolveItemMeta(itemId: ItemId): ItemMeta? {
        val itemProperties = itemPropertiesService.resolve(itemId) ?: return null
        return ItemMeta(itemProperties)
    }

    suspend fun resolvePendingItemMeta(itemId: ItemId, tokenUri: String): ItemMeta? {
        val itemProperties = rariblePropertiesResolver.resolveByTokenUri(itemId, tokenUri) ?: return null
        // Meta content will be resolved on the union-service.
        return ItemMeta(itemProperties)
    }
}
