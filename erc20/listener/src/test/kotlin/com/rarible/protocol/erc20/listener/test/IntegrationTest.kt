package com.rarible.protocol.erc20.listener.test

import com.rarible.core.test.ext.EthereumTest
import com.rarible.core.test.ext.KafkaTest
import com.rarible.core.test.ext.MongoTest
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Testcontainers

@Retention
@AutoConfigureJson
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = [
        "application.environment = e2e",
        "listener.blockchain = ethereum",
            "rarible.blockchain.monitoring = ethereum",
            "spring.cloud.service-registry.auto-registration.enabled = false",
            "spring.cloud.discovery.enabled = false",
            "rarible.common.jms-brokerUrls = localhost:\${random.int(5000,5100)}",
            "rarible.common.jms-eventTopic = protocol",
            "spring.cloud.consul.config.enabled = false",
            "logging.logstash.tcp-socket.enabled = false"
    ]
)
@ActiveProfiles("integration")
@Import(TestConfiguration::class)
@MongoTest
@KafkaTest
@EthereumTest
@Testcontainers
annotation class IntegrationTest
