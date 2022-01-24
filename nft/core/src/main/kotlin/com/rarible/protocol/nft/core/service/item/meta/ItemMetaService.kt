package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemMeta

interface ItemMetaService {
    suspend fun getItemMetadata(itemId: ItemId, returnOnlyCachedMeta: Boolean = false): ItemMeta
    suspend fun resetMetadata(itemId: ItemId)
}
