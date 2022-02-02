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
            statuses.isNullOrEmpty() && featureFlags.hideInactiveOrders -> OrderStatusDto.values().toList().filter { it != OrderStatusDto.INACTIVE }
            statuses?.isNotEmpty() == true && featureFlags.hideInactiveOrders -> statuses.filter { it != OrderStatusDto.INACTIVE }
            else -> statuses
        }
    }
}
