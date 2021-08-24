package com.rarible.protocol.unlockable.api.subscriber.autoconfigure

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

internal const val PROTOCOL_ORDER_SUBSCRIBER = "protocol.unlockable.subscriber"

@ConfigurationProperties(PROTOCOL_ORDER_SUBSCRIBER)
@ConstructorBinding
data class UnlockableEventsSubscriberProperties(
    val brokerReplicaSet: String
)
