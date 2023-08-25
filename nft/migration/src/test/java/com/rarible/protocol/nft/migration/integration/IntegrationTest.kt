package com.rarible.protocol.nft.migration.integration

import com.rarible.core.test.ext.KafkaTest
import com.rarible.core.test.ext.MongoTest
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
        "common.blockchain = ethereum",
        "spring.cloud.service-registry.auto-registration.enabled = false",
        "spring.cloud.discovery.enabled = false",
        "spring.cloud.consul.config.enabled = false",
        "logging.logstash.tcp-socket.enabled = false",
        "node.host = localhost",
        "node.websocketUrl = localhost"
    ]
)
@ActiveProfiles("integration")
@Import(TestPropertiesConfiguration::class)
@Testcontainers
annotation class IntegrationTest
