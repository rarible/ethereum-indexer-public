package com.rarible.protocol.nftorder.listener.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("listener.event-producer")
data class NftOrderEventProducerProperties(
    val environment: String,
    val kafkaReplicaSet: String
)
