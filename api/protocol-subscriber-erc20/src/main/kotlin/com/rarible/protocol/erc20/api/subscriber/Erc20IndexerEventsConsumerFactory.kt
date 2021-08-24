package com.rarible.protocol.erc20.api.subscriber

import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.dto.Erc20BalanceEventDto
import com.rarible.protocol.dto.Erc20BalanceEventTopicProvider
import java.util.*

class Erc20IndexerEventsConsumerFactory(
    private val brokerReplicaSet: String,
    private val host: String,
    private val environment: String
) {
    fun createErc20BalanceEventsConsumer(consumerGroup: String, blockchain: Blockchain): Erc20KafkaConsumerArguments<Erc20BalanceEventDto> {
        return Erc20KafkaConsumerArguments(
            clientId = "${createClientIdPrefix(blockchain)}.erc20-indexer-balance-events-consumer",
            valueDeserializerClass = Erc20EventDtoDeserializer::class.java,
            consumerGroup = consumerGroup,
            defaultTopic = Erc20BalanceEventTopicProvider.getTopic(environment, blockchain.value),
            bootstrapServers = brokerReplicaSet
        )
    }

    private fun createClientIdPrefix(blockchain: Blockchain): String {
        return "$environment.${blockchain.value}.$host.${UUID.randomUUID()}"
    }
}
