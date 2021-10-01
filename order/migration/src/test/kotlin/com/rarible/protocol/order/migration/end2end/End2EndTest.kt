package com.rarible.protocol.order.migration.end2end

import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Testcontainers

@Retention
@AutoConfigureJson
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = [
        "application.environment = e2e",
        "spring.cloud.service-registry.auto-registration.enabled = false",
        "spring.cloud.discovery.enabled = false",
        "rarible.core.contract.enabled = true",
        "spring.cloud.consul.config.enabled = false",
        "logging.logstash.tcp-socket.enabled = false",
        "common.kafka-replica-set = localhost:\${random.int(5000,5100)}",
        "rarible.core.ethereum.url=test",
        "rarible.core.ethereum.web-socket-url=test",
        "node.hosts=localhost:8081",
        "node.webSocketHosts=localhost:8081",
        "kafka.hosts=localhost:8081"
    ]
)
@ActiveProfiles("e2e", "integration", "ethereum")
@Testcontainers
@EnableAutoConfiguration
annotation class End2EndTest
