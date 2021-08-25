package com.rarible.protocol.nftorder.core.data

import com.rarible.protocol.dto.OrderDto
import com.rarible.protocol.nftorder.core.model.Item
import java.math.BigInteger

data class ItemEnrichmentData(
    val sellers: Int,
    val totalStock: BigInteger,
    val bestBidOrder: OrderDto?,
    val bestSellOrder: OrderDto?,
    val unlockable: Boolean
) {

    fun isNotEmpty(): Boolean {
        return unlockable || bestBidOrder != null || bestSellOrder != null
    }

    companion object {
        fun isNotEmpty(item: Item): Boolean {
            return item.unlockable || item.bestBidOrder != null || item.bestSellOrder != null
        }
    }

}