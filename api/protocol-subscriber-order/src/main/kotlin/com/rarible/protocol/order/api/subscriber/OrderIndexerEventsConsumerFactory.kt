package com.rarible.protocol.order.api.subscriber

import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.core.kafka.json.JsonDeserializer
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.dto.OrderEventDto
import com.rarible.protocol.dto.OrderIndexerTopicProvider
import java.util.*

class OrderIndexerEventsConsumerFactory(
    private val brokerReplicaSet: String,
    private val blockchain: Blockchain,
    host: String,
    private val environment: String
) {
    private val clientIdPrefix = "$environment.${blockchain.value}.$host.${UUID.randomUUID()}"

    fun createOrderEventsConsumer(consumerGroup: String): RaribleKafkaConsumer<OrderEventDto> {
        return RaribleKafkaConsumer(
            clientId = "$clientIdPrefix.order-indexer-order-events-consumer",
            valueDeserializerClass = JsonDeserializer::class.java,
            valueClass = OrderEventDto::class.java,
            consumerGroup = consumerGroup,
            defaultTopic = OrderIndexerTopicProvider.getTopic(environment, blockchain.value) + ".global",
            bootstrapServers = brokerReplicaSet
        )
    }
}
