package com.rarible.protocol.order.listener.integration

import com.rarible.core.test.containers.KafkaTestContainer
import com.rarible.core.test.containers.MongodbTestContainer
import com.rarible.core.test.containers.OpenEthereumTestContainer

abstract class BaseListenerApplicationTest {
    init {
        System.setProperty(
            "parity.hosts", openEthereumTest.ethereumUrl().toString()
        )
        System.setProperty(
            "parityWebSocketUrls", openEthereumTest.ethereumWebSocketUrl().toString()
        )
        System.setProperty(
            "spring.data.mongodb.uri", mongoTest.connectionString()
        )
        System.setProperty(
            "spring.data.mongodb.database", "protocol"
        )
        System.setProperty(
            "protocol.nft.subscriber.broker-replica-set", kafkaTestContainer.kafkaBoostrapServers()
        )
        System.setProperty(
            "protocol.erc20.subscriber.broker-replica-set", kafkaTestContainer.kafkaBoostrapServers()
        )
        System.setProperty(
            "protocol.order.subscriber.broker-replica-set", kafkaTestContainer.kafkaBoostrapServers()
        )
        System.setProperty(
            "common.kafka-replica-set", kafkaTestContainer.kafkaBoostrapServers()
        )
        System.setProperty(
            "rarible.ethereum.parity.httpUrl", openEthereumTest.ethereumUrl().toString()
        )
    }
    companion object {
        val kafkaTestContainer = KafkaTestContainer()
        val openEthereumTest = OpenEthereumTestContainer()
        val mongoTest = MongodbTestContainer()
    }
}
