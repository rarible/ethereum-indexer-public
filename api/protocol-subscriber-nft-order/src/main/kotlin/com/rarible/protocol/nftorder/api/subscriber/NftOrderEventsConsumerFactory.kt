package com.rarible.protocol.nftorder.api.subscriber

import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.core.kafka.json.JsonDeserializer
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.dto.*
import java.util.*

class NftOrderEventsConsumerFactory(
    private val brokerReplicaSet: String,
    private val host: String,
    private val environment: String
) {
    fun createNftOrderActivityConsumer(consumerGroup: String, blockchain: Blockchain): RaribleKafkaConsumer<ActivityDto> {
        return RaribleKafkaConsumer(
            clientId = "${createClientIdPrefix(blockchain)}.nft-order-activity-consumer",
            valueDeserializerClass = JsonDeserializer::class.java,
            valueClass = ActivityDto::class.java,
            consumerGroup = consumerGroup,
            defaultTopic = ActivityTopicProvider.getTopic(environment, blockchain.value),
            bootstrapServers = brokerReplicaSet
        )
    }

    fun createNftOrderItemConsumer(consumerGroup: String, blockchain: Blockchain): RaribleKafkaConsumer<NftOrderItemEventDto> {
        return RaribleKafkaConsumer(
            clientId = "${createClientIdPrefix(blockchain)}.nft-order-item-consumer",
            valueDeserializerClass = JsonDeserializer::class.java,
            valueClass = NftOrderItemEventDto::class.java,
            consumerGroup = consumerGroup,
            defaultTopic = NftOrderItemEventTopicProvider.getTopic(environment, blockchain.value),
            bootstrapServers = brokerReplicaSet
        )
    }

    fun createNftOrderOwnershipConsumer(consumerGroup: String, blockchain: Blockchain): RaribleKafkaConsumer<NftOrderOwnershipEventDto> {
        return RaribleKafkaConsumer(
            clientId = "${createClientIdPrefix(blockchain)}.nft-order-ownership-consumer",
            valueDeserializerClass = JsonDeserializer::class.java,
            valueClass = NftOrderOwnershipEventDto::class.java,
            consumerGroup = consumerGroup,
            defaultTopic = NftOrderOwnershipEventTopicProvider.getTopic(environment, blockchain.value),
            bootstrapServers = brokerReplicaSet
        )
    }

    private fun createClientIdPrefix(blockchain: Blockchain): String {
        return "$environment.${blockchain.value}.$host.${UUID.randomUUID()}"
    }
}
