package com.rarible.protocol.nftorder.core.data

import com.rarible.protocol.dto.LegacyOrderDto
import com.rarible.protocol.dto.OrderDto
import com.rarible.protocol.dto.PlatformDto
import com.rarible.protocol.dto.RaribleV2OrderDto
import com.rarible.protocol.nftorder.core.model.ShortOrder

object RaribleOrderChecker {

    fun isRaribleOrder(order: ShortOrder): Boolean {
        return order.platform == PlatformDto.RARIBLE.name
    }

    fun isRaribleOrder(order: OrderDto): Boolean {
        return order is RaribleV2OrderDto || order is LegacyOrderDto
    }

}