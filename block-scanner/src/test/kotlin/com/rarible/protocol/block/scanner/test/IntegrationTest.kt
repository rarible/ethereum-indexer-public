package com.rarible.protocol.block.scanner.test

import com.rarible.core.test.ext.EthereumTest
import com.rarible.core.test.ext.KafkaTest
import com.rarible.core.test.ext.MongoTest
import com.rarible.core.test.ext.RedisTest
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Testcontainers

@Retention
@MongoTest
@KafkaTest
@RedisTest
@EthereumTest
@AutoConfigureJson
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = [
        "application.environment = e2e",
        "spring.cloud.service-registry.auto-registration.enabled = false",
        "spring.cloud.discovery.enabled = false",
        "spring.cloud.consul.config.enabled = false",
        "logging.logstash.tcp-socket.enabled = false",
        "logging.level.org.springframework.data.mongodb.core.ReactiveMongoTemplate=DEBUG"
    ]
)
@ActiveProfiles("integration",  "polygon")
@Import(TestPropertiesConfiguration::class)
@Testcontainers
annotation class IntegrationTest
