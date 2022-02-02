package com.rarible.protocol.order.core.converters.model

import com.rarible.protocol.dto.OrderStatusDto
import com.rarible.protocol.dto.PlatformDto
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties

class StatusFeaturedFilter(
    private val featureFlags: OrderIndexerProperties.FeatureFlags
) {

    fun filter(statuses: List<OrderStatusDto>?): List<OrderStatusDto>? {
        return when {
            statuses.isNullOrEmpty() && !featureFlags.hideInactiveOrders -> null
            statuses.isNullOrEmpty() && featureFlags.hideInactiveOrders -> OrderStatusDto.values().toList()
                .filter { !listOf(OrderStatusDto.INACTIVE, OrderStatusDto.HISTORICAL).contains(it) }
            statuses?.isNotEmpty() == true && featureFlags.hideInactiveOrders -> statuses
                .filter { !listOf(OrderStatusDto.INACTIVE, OrderStatusDto.HISTORICAL).contains(it) }
            else -> statuses
        }
    }

    fun emptyResponse(statuses: List<OrderStatusDto>?): Boolean {
        return statuses?.isNotEmpty() == true && statuses == listOf(OrderStatusDto.INACTIVE) && featureFlags.hideInactiveOrders
    }
}
