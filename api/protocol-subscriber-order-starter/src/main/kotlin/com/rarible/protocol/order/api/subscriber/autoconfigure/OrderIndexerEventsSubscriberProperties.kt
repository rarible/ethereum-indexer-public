package com.rarible.protocol.order.api.subscriber.autoconfigure

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

internal const val PROTOCOL_ORDER_SUBSCRIBER = "protocol.order.subscriber"

@ConfigurationProperties(PROTOCOL_ORDER_SUBSCRIBER)
@ConstructorBinding
data class OrderIndexerEventsSubscriberProperties(
    val brokerReplicaSet: String
)
