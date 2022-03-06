package com.rarible.protocol.nft.api.service.mint

import com.rarible.protocol.nft.core.model.ExtendedItem
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemLazyMint

interface MintService {
    suspend fun createLazyNft(lazyItemHistory: ItemLazyMint): ExtendedItem

    suspend fun burnLazyMint(itemId: ItemId)
}
