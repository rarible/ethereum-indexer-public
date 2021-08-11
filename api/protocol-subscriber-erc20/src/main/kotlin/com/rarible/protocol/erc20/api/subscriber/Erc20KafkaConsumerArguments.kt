package com.rarible.protocol.erc20.api.subscriber

import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.common.serialization.Deserializer

data class Erc20KafkaConsumerArguments<V>(
    val clientId: String,
    val consumerGroup: String,
    val valueDeserializerClass: Class<out Deserializer<V>>,
    val defaultTopic: String,
    val bootstrapServers: String,
    val offsetResetStrategy: OffsetResetStrategy = OffsetResetStrategy.LATEST
)
