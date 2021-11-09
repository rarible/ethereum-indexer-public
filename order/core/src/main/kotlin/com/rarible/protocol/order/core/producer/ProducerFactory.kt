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
    private val orderEventsClientId = "$environment.${blockchain.value}.protocol-order-events-importer"
    private val orderActivityClientId = "$environment.${blockchain.value}.protocol-order-activities-importer"

    private val auctionEventsClientId = "$environment.${blockchain.value}.protocol-auction-events-importer"
    private val auctionActivityClientId = "$environment.${blockchain.value}.protocol-auction-activities-importer"

    fun createOrderEventsProducer(): RaribleKafkaProducer<OrderEventDto> {
        return RaribleKafkaProducer(
            clientId = orderEventsClientId,
            valueSerializerClass = JsonSerializer::class.java,
            valueClass = OrderEventDto::class.java,
            defaultTopic = OrderIndexerTopicProvider.getOrderUpdateTopic(environment, blockchain.value),
            bootstrapServers = kafkaReplicaSet
        )
    }

    fun createOrderPriceUpdateEventsProducer(): RaribleKafkaProducer<NftOrdersPriceUpdateEventDto> {
        return RaribleKafkaProducer(
            clientId = orderEventsClientId,
            valueSerializerClass = JsonSerializer::class.java,
            valueClass = NftOrdersPriceUpdateEventDto::class.java,
            defaultTopic = OrderIndexerTopicProvider.getPriceUpdateTopic(environment, blockchain.value),
            bootstrapServers = kafkaReplicaSet
        )
    }

    fun createGlobalOrderEventsProducer(): RaribleKafkaProducer<OrderEventDto> {
        return RaribleKafkaProducer(
            clientId = orderEventsClientId,
            valueSerializerClass = JsonSerializer::class.java,
            valueClass = OrderEventDto::class.java,
            defaultTopic = "${OrderIndexerTopicProvider.getOrderUpdateTopic(environment, blockchain.value)}.global",
            bootstrapServers = kafkaReplicaSet
        )
    }

    fun createOrderActivitiesProducer(): RaribleKafkaProducer<ActivityDto> {
        return RaribleKafkaProducer(
            clientId = orderActivityClientId,
            valueSerializerClass = JsonSerializer::class.java,
            valueClass = ActivityDto::class.java,
            defaultTopic = ActivityTopicProvider.getTopic(environment, blockchain.name.toLowerCase()),
            bootstrapServers = kafkaReplicaSet
        )
    }

    fun createAuctionEventsProducer(): RaribleKafkaProducer<AuctionEventDto> {
        return RaribleKafkaProducer(
            clientId = auctionEventsClientId,
            valueSerializerClass = JsonSerializer::class.java,
            valueClass = AuctionEventDto::class.java,
            defaultTopic = OrderIndexerTopicProvider.getAuctionUpdateTopic(environment, blockchain.value),
            bootstrapServers = kafkaReplicaSet
        )
    }

    fun createAuctionActivitiesProducer(): RaribleKafkaProducer<ActivityDto> {
        return RaribleKafkaProducer(
            clientId = auctionActivityClientId,
            valueSerializerClass = JsonSerializer::class.java,
            valueClass = ActivityDto::class.java,
            defaultTopic = ActivityTopicProvider.getTopic(environment, blockchain.name.toLowerCase()),
            bootstrapServers = kafkaReplicaSet
        )
    }
}
