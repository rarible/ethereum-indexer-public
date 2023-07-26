package com.rarible.protocol.order.core.integration

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
    }
    companion object {
        val openEthereumTest = OpenEthereumTestContainer()
        val mongoTest = MongodbTestContainer()
    }
}
