package com.rarible.protocol.nftorder.core.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("common.event-producer")
data class EventProducerProperties(
    val environment: String,
    val kafkaReplicaSet: String
)
