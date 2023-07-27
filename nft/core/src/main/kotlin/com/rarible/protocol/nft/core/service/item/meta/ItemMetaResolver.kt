package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemMeta
import org.springframework.stereotype.Component

@Component
class ItemMetaResolver(
    private val itemPropertiesService: ItemPropertiesService
) {

    suspend fun resolveItemMeta(itemId: ItemId): ItemMeta? {
        val itemProperties = itemPropertiesService.resolve(itemId) ?: return null
        return ItemMeta(itemProperties)
    }
}
