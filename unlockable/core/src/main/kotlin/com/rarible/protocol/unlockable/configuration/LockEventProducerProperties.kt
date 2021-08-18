package com.rarible.protocol.unlockable.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("rarible.event-producer")
data class LockEventProducerProperties(
    val environment: String,
    val kafkaReplicaSet: String
)
