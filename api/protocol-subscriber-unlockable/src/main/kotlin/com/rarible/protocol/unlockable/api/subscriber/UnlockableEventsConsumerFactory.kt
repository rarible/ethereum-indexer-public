package com.rarible.protocol.unlockable.api.subscriber

import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.core.kafka.json.JsonDeserializer
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.dto.UnlockableEventDto
import com.rarible.protocol.dto.UnlockableTopicProvider
import java.util.*

class UnlockableEventsConsumerFactory(
    private val brokerReplicaSet: String,
    private val host: String,
    private val environment: String
) {
    fun createUnlockableEventsConsumer(consumerGroup: String, blockchain: Blockchain): RaribleKafkaConsumer<UnlockableEventDto> {
        return RaribleKafkaConsumer(
            clientId = "${createClientIdPrefix(blockchain)}.lock-event-consumer",
            valueDeserializerClass = JsonDeserializer::class.java,
            valueClass = UnlockableEventDto::class.java,
            consumerGroup = consumerGroup,
            defaultTopic = UnlockableTopicProvider.getTopic(environment, blockchain.value),
            bootstrapServers = brokerReplicaSet
        )
    }

    private fun createClientIdPrefix(blockchain: Blockchain): String {
        return "$environment.${blockchain.value}.$host.${UUID.randomUUID()}"
    }
}
