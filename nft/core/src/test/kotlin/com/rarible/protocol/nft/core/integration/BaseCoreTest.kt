package com.rarible.protocol.nft.core.integration

import com.rarible.core.test.containers.KafkaTestContainer
import com.rarible.core.test.containers.MongodbTestContainer
import com.rarible.core.test.containers.OpenEthereumTestContainer

abstract class BaseCoreTest {
    init {
        System.setProperty(
            "parityUrls", openEthereumTest.ethereumUrl().toString()
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
            "kafka.hosts", kafkaTest.kafkaBoostrapServers()
        )
    }
    companion object {
        val kafkaTest = KafkaTestContainer()
        val openEthereumTest = OpenEthereumTestContainer()
        val mongoTest = MongodbTestContainer()
    }
}
