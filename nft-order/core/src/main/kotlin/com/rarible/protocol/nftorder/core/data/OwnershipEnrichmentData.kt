package com.rarible.protocol.nftorder.core.data

import com.rarible.protocol.dto.OrderDto

class OwnershipEnrichmentData(
    val bestSellOrder: OrderDto?
) {

    fun isNotEmpty(): Boolean {
        return bestSellOrder != null
    }
}