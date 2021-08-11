package com.rarible.protocol.nftorder.api.subscriber

import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.core.kafka.json.JsonDeserializer
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.dto.ActivityDto
import com.rarible.protocol.dto.ActivityTopicProvider
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
}