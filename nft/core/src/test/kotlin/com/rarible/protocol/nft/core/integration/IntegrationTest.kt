package com.rarible.protocol.nft.core.integration

import com.rarible.protocol.nft.core.MockContext
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.junit.jupiter.Testcontainers

@Retention
@AutoConfigureJson
@ContextConfiguration(classes = [MockContext::class])
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = [
        "application.environment = e2e",
        "spring.application.name = test",
        "common.blockchain = ethereum",
        "spring.cloud.service-registry.auto-registration.enabled = false",
        "spring.cloud.discovery.enabled = false",
        "rarible.common.jms-brokerUrls = localhost:\${random.int(5000,5100)}",
        "rarible.common.jms-eventTopic = protocol",
        "spring.cloud.consul.config.enabled = false",
        "logging.logstash.tcp-socket.enabled = false"
    ]
)
@Import(TestPropertiesConfiguration::class)
@ActiveProfiles("integration", "reduce-v2")
@Testcontainers
annotation class IntegrationTest
