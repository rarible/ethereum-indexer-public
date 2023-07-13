package com.rarible.protocol.order.core.producer

import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.kafka.json.JsonSerializer
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.dto.ActivityTopicProvider
import com.rarible.protocol.dto.AuctionEventDto
import com.rarible.protocol.dto.EthActivityEventDto
import com.rarible.protocol.dto.NftOrdersPriceUpdateEventDto
import com.rarible.protocol.dto.OrderEventDto
import com.rarible.protocol.dto.OrderIndexerTopicProvider

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

    fun createOrderActivitiesProducer(): RaribleKafkaProducer<EthActivityEventDto> {
        return RaribleKafkaProducer(
            clientId = orderActivityClientId,
            valueSerializerClass = JsonSerializer::class.java,
            valueClass = EthActivityEventDto::class.java,
            defaultTopic = ActivityTopicProvider.getActivityTopic(environment, blockchain.value),
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

    fun createAuctionActivitiesProducer(): RaribleKafkaProducer<EthActivityEventDto> {
        return RaribleKafkaProducer(
            clientId = auctionActivityClientId,
            valueSerializerClass = JsonSerializer::class.java,
            valueClass = EthActivityEventDto::class.java,
            defaultTopic = ActivityTopicProvider.getActivityTopic(environment, blockchain.value),
            bootstrapServers = kafkaReplicaSet
        )
    }
}
