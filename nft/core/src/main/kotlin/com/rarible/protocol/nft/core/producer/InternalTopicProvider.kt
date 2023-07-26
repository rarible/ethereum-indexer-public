package com.rarible.protocol.nft.core.producer

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.core.kafka.json.JsonDeserializer
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.nft.core.model.ActionEvent
import java.util.UUID

object InternalTopicProvider {
    const val VERSION = "v1"

    fun getItemActionTopic(environment: String, blockchain: String): String {
        return "protocol.$environment.$blockchain.item.internal.action"
    }

    fun createInternalActionEventConsumer(
        applicationEnvironmentInfo: ApplicationEnvironmentInfo,
        blockchain: Blockchain,
        kafkaReplicaSet: String
    ): RaribleKafkaConsumer<ActionEvent> {
        val environment = applicationEnvironmentInfo.name
        val host = applicationEnvironmentInfo.host
        val consumerGroup = "$environment.protocol.${blockchain.value}.nft.indexer.internal.action"
        val clientIdPrefix = "$environment.${blockchain.value}.$host.${UUID.randomUUID()}"
        return RaribleKafkaConsumer(
            clientId = "$clientIdPrefix.nft.indexer.action.internal",
            valueDeserializerClass = JsonDeserializer::class.java,
            valueClass = ActionEvent::class.java,
            consumerGroup = consumerGroup,
            defaultTopic = getItemActionTopic(environment, blockchain.value),
            bootstrapServers = kafkaReplicaSet
        )
    }
}
