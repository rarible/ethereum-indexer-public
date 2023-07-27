package com.rarible.protocol.erc20.core.integration

import com.rarible.core.test.ext.MongoCleanup
import com.rarible.core.test.ext.MongoTest
import com.rarible.protocol.erc20.core.MockContext
import com.rarible.protocol.erc20.core.configuration.CoreConfiguration
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.junit.jupiter.Testcontainers

@Retention
@AutoConfigureJson
@ContextConfiguration(classes = [MockContext::class, CoreConfiguration::class])
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = [
        "application.environment = e2e",
        "spring.application.name = test",
        "listener.blockchain = ethereum",
        "common.blockchain = ethereum",
        "common.kafkaReplicaSet = ",
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
@Import(TestPropertiesConfiguration::class)
@Testcontainers
@MongoTest
@MongoCleanup
annotation class IntegrationTest
