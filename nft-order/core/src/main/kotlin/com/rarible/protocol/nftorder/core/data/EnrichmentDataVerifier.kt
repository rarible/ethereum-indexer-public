package com.rarible.protocol.nftorder.core.data

import com.rarible.protocol.nftorder.core.model.Item
import com.rarible.protocol.nftorder.core.model.Ownership

object EnrichmentDataVerifier {

    fun isOwnershipNotEmpty(ownership: Ownership): Boolean {
        return ownership.bestSellOrder != null
    }

    fun isItemNotEmpty(item: Item): Boolean {
        return item.unlockable || item.bestBidOrder != null || item.bestSellOrder != null
    }

}