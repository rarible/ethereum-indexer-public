package com.rarible.protocol.order.core.integration

import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.junit.jupiter.Testcontainers

@Retention
@AutoConfigureJson
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
        "logging.logjson.enabled = false",
        "logging.logstash.tcp-socket.enabled = false"
    ]
)
@ActiveProfiles("integration")
@Import(TestPropertiesConfiguration::class)
@ContextConfiguration(classes = [MockContext::class])
@Testcontainers
@EnableAutoConfiguration
annotation class IntegrationTest
