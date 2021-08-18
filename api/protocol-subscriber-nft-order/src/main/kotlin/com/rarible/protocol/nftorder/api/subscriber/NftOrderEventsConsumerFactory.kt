package com.rarible.protocol.nftorder.api.subscriber

import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.core.kafka.json.JsonDeserializer
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.dto.*
import java.util.*

class NftOrderEventsConsumerFactory(
    private val brokerReplicaSet: String,
    private val blockchain: Blockchain,
    host: String,
    private val environment: String
) {
    private val clientIdPrefix = "$environment.${blockchain.value}.$host.${UUID.randomUUID()}"

    fun createNftOrderActivityConsumer(consumerGroup: String): RaribleKafkaConsumer<ActivityDto> {
        return RaribleKafkaConsumer(
            clientId = "$clientIdPrefix.nft-order-activity-consumer",
            valueDeserializerClass = JsonDeserializer::class.java,
            valueClass = ActivityDto::class.java,
            consumerGroup = consumerGroup,
            defaultTopic = ActivityTopicProvider.getTopic(environment, blockchain.value),
            bootstrapServers = brokerReplicaSet
        )
    }

    fun createNftOrderItemConsumer(consumerGroup: String): RaribleKafkaConsumer<NftOrderItemEventDto> {
        return RaribleKafkaConsumer(
            clientId = "$clientIdPrefix.nft-order-item-consumer",
            valueDeserializerClass = JsonDeserializer::class.java,
            valueClass = NftOrderItemEventDto::class.java,
            consumerGroup = consumerGroup,
            defaultTopic = NftOrderItemEventTopicProvider.getTopic(environment, blockchain.value),
            bootstrapServers = brokerReplicaSet
        )
    }

    fun createNftOrderOwnershipConsumer(consumerGroup: String): RaribleKafkaConsumer<NftOrderOwnershipEventDto> {
        return RaribleKafkaConsumer(
            clientId = "$clientIdPrefix.nft-order-ownership-consumer",
            valueDeserializerClass = JsonDeserializer::class.java,
            valueClass = NftOrderOwnershipEventDto::class.java,
            consumerGroup = consumerGroup,
            defaultTopic = NftOrderOwnershipEventTopicProvider.getTopic(environment, blockchain.value),
            bootstrapServers = brokerReplicaSet
        )
    }
}