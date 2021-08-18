package com.rarible.protocol.nftorder.core.data

import com.rarible.protocol.dto.OrderDto
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

}