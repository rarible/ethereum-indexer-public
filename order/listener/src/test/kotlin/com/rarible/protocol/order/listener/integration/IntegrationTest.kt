package com.rarible.protocol.order.listener.integration

import com.rarible.core.test.ext.EthereumTest
import com.rarible.core.test.ext.KafkaTest
import com.rarible.core.test.ext.MongoTest
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Testcontainers

@Retention
@MongoTest
@KafkaTest
@AutoConfigureJson
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = [
            "application.environment = e2e",
            "listener.blockchain = ethereum",
            "spring.cloud.service-registry.auto-registration.enabled = false",
            "spring.cloud.discovery.enabled = false",
            "rarible.common.jms-brokerUrls = localhost:\${random.int(5000,5100)}",
            "rarible.common.jms-eventTopic = protocol",
            "rarible.core.contract.enabled = true",
            "spring.cloud.consul.config.enabled = false",
            "logging.logstash.tcp-socket.enabled = false",
            "logging.logjson.enabled = false",
    ]
)
@EthereumTest
@ActiveProfiles("integration")
@Import(TestConfiguration::class)
@Testcontainers
@EnableAutoConfiguration
annotation class IntegrationTest
