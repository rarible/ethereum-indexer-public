package com.rarible.protocol.order.core.producer

import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.kafka.json.JsonSerializer
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.dto.*


class ProducerFactory(
    private val kafkaReplicaSet: String,
    private val blockchain: Blockchain,
    private val environment: String
) {
    private val eventsClientId = "$environment.${blockchain.value}.protocol-order-events-importer"
    private val activityClientId = "$environment.${blockchain.value}.protocol-order-activities-importer"

    fun createOrderEventsProducer(): RaribleKafkaProducer<OrderEventDto> {
        return RaribleKafkaProducer(
            clientId = eventsClientId,
            valueSerializerClass = JsonSerializer::class.java,
            valueClass = OrderEventDto::class.java,
            defaultTopic = OrderIndexerTopicProvider.getUpdateTopic(environment, blockchain.value),
            bootstrapServers = kafkaReplicaSet
        )
    }

    fun createOrderPriceUpdateEventsProducer(): RaribleKafkaProducer<NftOrdersPriceUpdateEventDto> {
        return RaribleKafkaProducer(
            clientId = eventsClientId,
            valueSerializerClass = JsonSerializer::class.java,
            valueClass = NftOrdersPriceUpdateEventDto::class.java,
            defaultTopic = OrderIndexerTopicProvider.getPriceUpdateTopic(environment, blockchain.value),
            bootstrapServers = kafkaReplicaSet
        )
    }

    fun createGlobalOrderEventsProducer(): RaribleKafkaProducer<OrderEventDto> {
        return RaribleKafkaProducer(
            clientId = eventsClientId,
            valueSerializerClass = JsonSerializer::class.java,
            valueClass = OrderEventDto::class.java,
            defaultTopic = "${OrderIndexerTopicProvider.getUpdateTopic(environment, blockchain.value)}.global",
            bootstrapServers = kafkaReplicaSet
        )
    }

    fun createOrderActivitiesProducer(): RaribleKafkaProducer<ActivityDto> {
        return RaribleKafkaProducer(
            clientId = activityClientId,
            valueSerializerClass = JsonSerializer::class.java,
            valueClass = ActivityDto::class.java,
            defaultTopic = ActivityTopicProvider.getTopic(environment, blockchain.name.toLowerCase()),
            bootstrapServers = kafkaReplicaSet
        )
    }
}
