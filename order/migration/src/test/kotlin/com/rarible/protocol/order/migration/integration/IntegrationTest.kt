package com.rarible.protocol.order.migration.integration

import com.rarible.core.test.ext.MongoCleanup
import com.rarible.core.test.ext.MongoTest
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Testcontainers

@Retention
@MongoTest
@MongoCleanup
@AutoConfigureJson
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = [
        "application.environment = e2e",
        "common.blockchain = ethereum",
        "spring.cloud.service-registry.auto-registration.enabled = false",
        "spring.cloud.discovery.enabled = false",
        "rarible.common.jms-brokerUrls = localhost:\${random.int(5000,5100)}",
        "rarible.common.jms-eventTopic = protocol",
        "spring.cloud.consul.config.enabled = false",
        "logging.logstash.tcp-socket.enabled = false"
    ]
)
@ActiveProfiles("integration")
@Import(TestPropertiesConfiguration::class)
@Testcontainers
annotation class IntegrationTest
