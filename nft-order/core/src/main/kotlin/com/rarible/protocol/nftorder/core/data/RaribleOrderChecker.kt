package com.rarible.protocol.nftorder.core.data

import com.rarible.protocol.dto.LegacyOrderDto
import com.rarible.protocol.dto.OrderDto
import com.rarible.protocol.dto.RaribleV2OrderDto

object RaribleOrderChecker {

    fun isRaribleOrder(order: OrderDto): Boolean {
        return order is RaribleV2OrderDto || order is LegacyOrderDto
    }

}