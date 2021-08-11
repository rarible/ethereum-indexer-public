package com.rarible.protocol.erc20.core.producer

import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.kafka.json.JsonSerializer
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.dto.Erc20BalanceEventDto
import com.rarible.protocol.dto.Erc20BalanceEventTopicProvider

class ProducerFactory(
    private val kafkaReplicaSet: String,
    private val blockchain: Blockchain,
    private val environment: String
) {
    private val clientId = "$environment.${blockchain.value}.protocol-erc20-events-importer"

    fun createErc20EventsProducer(): RaribleKafkaProducer<Erc20BalanceEventDto> {
        return RaribleKafkaProducer(
            clientId = clientId,
            valueSerializerClass = JsonSerializer::class.java,
            valueClass = Erc20BalanceEventDto::class.java,
            defaultTopic = Erc20BalanceEventTopicProvider.getTopic(environment, blockchain.value),
            bootstrapServers = kafkaReplicaSet
        )
    }
}
