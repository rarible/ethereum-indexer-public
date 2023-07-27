package com.rarible.protocol.order.api.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

internal const val RARIBLE_PROTOCOL_ORDER_API = "api"

@ConstructorBinding
@ConfigurationProperties(RARIBLE_PROTOCOL_ORDER_API)
data class OrderIndexerApiProperties(
    val skipHeavyRequest: Boolean = false,
    val maxOrderEndDate: Long? = null,
    val settings: OrderSettingsProperties = OrderSettingsProperties()
)

data class OrderSettingsProperties(
    val fees: Map<String, Int> = emptyMap()
)
