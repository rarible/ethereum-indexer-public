package com.rarible.protocol.nftorder.core.data

import com.rarible.protocol.dto.OrderDto
import com.rarible.protocol.nftorder.core.model.Ownership

class OwnershipEnrichmentData(
    val bestSellOrder: OrderDto?
) {

    fun isNotEmpty(): Boolean {
        return bestSellOrder != null
    }

    companion object {
        fun isNotEmpty(ownership: Ownership): Boolean {
            return ownership.bestSellOrder != null
        }
    }
}