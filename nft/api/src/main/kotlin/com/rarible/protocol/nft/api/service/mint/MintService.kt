package com.rarible.protocol.nft.api.service.mint

import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemLazyMint

interface MintService {
    suspend fun createLazyNft(lazyItemHistory: ItemLazyMint): Item

    suspend fun burnLazyMint(itemId: ItemId)
}



