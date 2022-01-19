package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties

/**
 * NFT metadata resolver responsible for fetching metadata [ItemProperties]
 * from external services (either from the Internet by "tokenURI") or from the specific contracts.
 */
interface ItemPropertiesResolver {
    val name: String
    suspend fun resolve(itemId: ItemId): ItemProperties?
    suspend fun reset(itemId: ItemId) = Unit
}
